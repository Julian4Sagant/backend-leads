# Proposal

## Change ID

`leads-api-crear-evaluacion-puntaje`

## Why

n8n ya completa el chat de 12 preguntas, pero todavía no hay un servicio que
calcule la prioridad del lead. Este change crea la primera versión usable de
leads-api: POST /evaluar, protegido por clave, que valida la solicitud, aplica
las reglas de puntaje y devuelve la prioridad con sus motivos y la versión de
las reglas. La decisión es determinística, explicable y auditable, y la IA no
participa en ella.

## What Changes

- Nuevo endpoint POST /evaluar con el contrato acordado con n8n.
- Validación completa de la solicitud: campos requeridos, tipos, valores
  permitidos, formato básico de números y rangos. Todos los errores se
  devuelven juntos en un solo 400.
- Motor de reglas con Drools: las 7 reglas de puntaje (urgencia,
  documentación, destino del préstamo, situación laboral, antigüedad,
  clearing y carga de deuda), con puntajes y umbrales en `application.yml`.
- Score con recorte a 0 y a 100 y su motivo informativo. Prioridad ALTA,
  MEDIA o BAJA según el score. Motivos en orden fijo.
- `version_reglas` en cada respuesta.
- Clave de API obligatoria (X-API-Key) en todos los endpoints. La app no
  arranca sin una clave válida.
- Errores como ProblemDetail con un catálogo de `type` en URN.
- Logs sin datos del cuerpo, salvo `session_id` con formato válido.
- Se saca lombok y se agrega Drools al `pom.xml`.
- **BREAKING** para n8n: cambia el contrato de POST /evaluar (ver "Contratos
  afectados").

## Contratos afectados

**POST /evaluar** (cambio incompatible respecto del n8n actual):

| Campo | Dirección | Cambio |
| --- | --- | --- |
| `nombre` | request | **Sale** (minimización de datos). Si n8n lo sigue mandando, se ignora como cualquier campo extra |
| `edad` | request | **Entra**, requerido, entero de 14 a 100 |
| `antiguedad_meses` | request | **Entra**, requerido, entero de 0 a 600 |
| `version_reglas` | response | **Entra**, 12 caracteres hexadecimales |

El resto del request (`session_id`, `motivo`, `monto`, `plazo_meses`,
`urgencia`, `situacion_laboral`, `ingreso_mensual`, `cuota_deudas`,
`clearing`, `documentacion`) y de la response (`prioridad`, `score`,
`motivos`) no cambia de forma. n8n se actualiza antes de la primera conexión;
todavía no hay producción.

**Errores:** todas las respuestas de error pasan a ser
`application/problem+json;charset=UTF-8` con un `type` del catálogo
`urn:leads-api:error:<caso>`. n8n debe mandar `Content-Type:
application/json` (si no, 415).

## Capabilities

### New Capabilities

- `evaluacion-leads`: contrato de POST /evaluar, validación de la solicitud,
  reglas de puntaje, score, prioridad, motivos y `version_reglas`.
- `acceso-api`: clave de API, endpoints expuestos, formato y catálogo de
  errores, y qué se registra en los logs.

### Modified Capabilities

Ninguna (no hay specs previas).

## Decisiones confirmadas

Confirmadas explícitamente por Julian en el pedido inicial (decisiones 1 a 24
de 2026-10-09) y en el explore del change 1 (2026-10-13):

- Las reglas y puntajes de la especificación inicial; prioridad ALTA con
  score >= 60, MEDIA de 30 a 59 y BAJA con menos de 30.
- Tabla de máximos por situación laboral (decisión 1) y sus consecuencias:
  con `clearing = si` nadie llega a ALTA; un estudiante llega a ALTA solo con
  el puntaje perfecto; un desempleado nunca llega a ALTA.
- Recorte a 0 y a 100 con motivo final informativo (decisión 2); carga con
  BigDecimal sin dividir, cuota estimada sin intereses (decisiones 4 y 5);
  orden fijo de motivos (decisión 6); guion ASCII y UTF-8 (decisión 7).
- Validación: mapeo explícito de códigos, todos los errores juntos, campos
  extra ignorados, rangos y formato de `session_id` (decisiones 10 a 12 y 14).
- Clave de API en X-API-Key desde `LEADS_API_KEY`, comparación en tiempo
  constante, sin springdoc ni actuator (decisiones 16 y 17).
- Puntajes y umbrales en `application.yml` validados al arrancar (decisión
  21); servicio sin estado ni idempotencia (decisión 24).
- Motor de reglas: Drools 10.2.0. Una regla DRL por tramo, agrupadas por un
  identificador estable de regla de negocio; el DRL emite códigos y es solo
  ASCII; Java arma los textos, ordena, calcula score y prioridad.
- `version_reglas` = SHA-256 de configuración canónica + DRL con finales de
  línea LF + constante de revisión de la lógica en Java, truncado a 12 hex.
- Clave con filtro propio (sin Spring Security), de al menos 32 caracteres
  ASCII imprimibles y sin espacios.
- `type` en URN `urn:leads-api:error:<caso>`, incluido `json-invalido`;
  `codigo` de error de campo con guion.
- (propose) Se agrega `tipo-de-contenido-no-soportado` (415) para un
  Content-Type distinto de `application/json`. El header Accept se ignora:
  el servicio responde siempre JSON y nunca 406.
- Logging: `session_id` válido junto a prioridad, score y `version_reglas`, o
  junto al `type` y el estado del error; nunca valores del cuerpo.
- `charset=UTF-8` explícito en las dos respuestas. Se saca lombok.

## Supuestos pendientes

Propuestos en este change; necesitan confirmación antes de aprobar:

1. **Textos de los motivos** que la especificación no fijó (urgencia,
   documentación, destino no definido, situación laboral, antigüedad y
   clearing). Están en la spec `evaluacion-leads`, requirement de cada regla.
2. **Cada regla de puntaje emite siempre exactamente un motivo**, también
   cuando suma 0 (por ejemplo "Sin documentación: 0").
3. **Formato del puntaje en el texto:** `+N` si es positivo, `-N` si es
   negativo y `0` sin signo.
4. **Números en este change:** se aceptan como número JSON o string numérico;
   se rechazan los que no son decimales, los enteros con parte decimal y los
   importes con más de 2 decimales o 12 dígitos enteros. Las regex exactas y
   los casos de la decisión 13 (`1e3`, `+5`, `.5`, espacios, etc.) quedan para
   `leads-api-endurecer-formato-numerico`.
5. **Bordes de rangos (decisión 15) en este change** y no en el change 3,
   porque las reglas de la spec piden escenario en el borde de cada límite.
6. **Códigos de dominio exactos:** distinguen mayúsculas y no se recortan
   espacios (`"Auto"` y `" auto"` son `valor-no-permitido`).
7. **`cuerpo-demasiado-grande`** queda registrado en el catálogo de `type`
   desde este change, pero su comportamiento llega con
   `leads-api-limitar-tamano-cuerpo`.
8. **JSON válido que no es un objeto** (por ejemplo `[1, 2]`) da
   `validacion` con `tipo-invalido` en el campo `$`, no `json-invalido`,
   porque el cuerpo sí se pudo leer como JSON.
9. **Textos de los errores de campo (`mensaje`)**: los define el diseño y no
   forman parte del contrato; n8n debe decidir por `type` y `codigo`, nunca
   por el texto.

## Fuera de alcance

- Regla excluyente de edad, inelegibilidad y su precedencia sobre el recorte
  (`leads-api-agregar-elegibilidad-edad`). En este change `edad` se valida
  pero no afecta el resultado.
- Regex estrictas de números, distinción fina de formatos y límite de
  longitud de números en el parser (`leads-api-endurecer-formato-numerico`).
- Límite de 16 KB, 413 y orden 401 > 413 > 400
  (`leads-api-limitar-tamano-cuerpo`).
- Cambios en n8n o en Google Sheets.
- Persistencia, idempotencia y rate limiting.

## Estrategia de testing

- **Tablas de casos por regla** con el borde exacto de cada umbral (carga en
  0,3 y 0,5; antigüedad en 5, 6, 11 y 12; prioridad en 60 y 30).
- **Tests HTTP con MockMvc** para cada escenario de la spec, con clave
  ficticia; respuesta leída como bytes UTF-8.
- **Tests de arranque** con configuraciones inválidas (clave, coherencia de
  puntajes) que verifican que el contexto no levanta.
- **Configuración de prueba** para lo que la real no alcanza (suma mayor que
  100, scores 59 y 29: los puntajes reales son múltiplos de 5).
- **Tests de logs** con captura de salida: sin valores del cuerpo y sin
  `session_id` inválido.
- **Tests del motor:** orden completo de motivos con el motivo final una sola
  vez, exactamente un motivo por regla de negocio, DRL solo ASCII, tabla de
  máximos calculada desde la configuración efectiva, y los tests de
  `version_reglas` (incluido el de cierre sobre los identificadores de regla).

## Aprobadores requeridos

- Julian: aprueba la propuesta con un comentario en el PR y revisa el código.

## Impact

- Código: nuevo `evaluacion/`, `seguridad/` y `comun/` bajo
  `com.sagant.leads.leads_api`; reglas en `src/main/resources/reglas/`.
- Configuración: `application.yml` reemplaza a `application.properties`;
  variable de entorno `LEADS_API_KEY` obligatoria (también en desarrollo con
  ngrok).
- Dependencias: entran `drools-engine` y `drools-mvel` 10.2.0 (unas 30
  transitivas, jar de ~23 MB); sale lombok.
- Sistemas: n8n debe adaptar el payload y leer `version_reglas`.
- Docs: `docs/roadmap.md` (bordes de rangos pasan al change 1).
