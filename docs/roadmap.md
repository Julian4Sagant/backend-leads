# Roadmap de leads-api

Plan corto de changes para construir leads-api, el servicio sin estado que
recibe una solicitud de préstamo ya normalizada por n8n y devuelve la prioridad
calculada por el motor de reglas. Surge del explore inicial (2026-10-09). Las
verificaciones técnicas están en [notas-tecnicas-leads.md](notas-tecnicas-leads.md).

Estado: ningún change iniciado. `openspec/specs/` está vacío.

## Capabilities

| Capability | Qué cubre la spec |
| --- | --- |
| `evaluacion-leads` | Contrato de POST /evaluar (request, response, validación de campos, formatos y rangos); reglas excluyentes y de puntaje; prioridad; score con recortes y ajuste por inelegibilidad; motivos y su orden; tabla de máximos por situación laboral; `version_reglas` |
| `acceso-api` | Clave X-API-Key (401, arranque sin clave, comparación en tiempo constante); sin actuator ni springdoc; 404 y 405 con clave; límite de 16 KB (413); orden 401 > 413 > 400; formato de errores ProblemDetail y su tabla de `type` |

El formato de errores vive en `acceso-api` para que la tabla de `type` esté en
un solo lugar. `evaluacion-leads` la referencia.

## Changes

```
  +-------------------------------------------+
  | 1. leads-api-crear-evaluacion-puntaje     |
  +-------------------------------------------+
        |                 |                |
        v                 v                v
  +-------------+  +---------------+  +-------------+
  | 2. agregar- |  | 3. endurecer- |  | 4. limitar- |
  | elegibilidad|  | formato-      |  | tamano-     |
  | -edad       |  | numerico      |  | cuerpo      |
  +-------------+  +---------------+  +-------------+

  Orden recomendado: 1, 2, 3, 4.  2, 3 y 4 dependen solo de 1.
```

### 1. `leads-api-crear-evaluacion-puntaje`
Rebanada base: POST /evaluar de punta a punta.
- Clave de API (filtro, 401, la app no arranca sin `LEADS_API_KEY`), sin
  actuator ni springdoc, 404/405 con clave válida.
- ProblemDetail desde un único `@RestControllerAdvice`, con la tabla de `type`.
- Contrato nuevo. **Cambio incompatible para n8n**, a declarar en "Contratos
  afectados": sale `nombre` del request; entran `edad` y `antiguedad_meses`
  (requeridos); entra `version_reglas` en la response.
- Validación: requeridos, valores permitidos de cada código, rangos, todos los
  errores juntos, campos extra ignorados.
- Las 7 reglas de puntaje (`ReglaPuntaje`), prioridad, recorte 0/100 con su
  motivo, orden de motivos, carga con BigDecimal sin dividir.
- Puntajes y umbrales en `application.yml` con validación de coherencia;
  `version_reglas` y sus tests; tabla de máximos y su test; UTF-8.
- Decisiones: 1, 2, 4 a 12, 14, 16, 17, 21 a 24.
- El DTO usa desde el inicio la representación "cruda" de los números (notas,
  punto c), para que el change 3 no tenga que rehacerlo.

### 2. `leads-api-agregar-elegibilidad-edad`
- Interfaz `ReglaExcluyente` y la regla de edad (< 18: BAJA, score 0).
- Motivo cuando pasa ("Mayor de edad: elegible").
- Precedencia del ajuste por inelegibilidad sobre el recorte, con sus tres
  escenarios. Bordes 17 y 18.
- Decisión: 3. Depende de 1. `edad` ya se valida desde el change 1 (14 a 100),
  pero hasta acá no afecta el resultado.

### 3. `leads-api-endurecer-formato-numerico`
- Una misma regex por campo para número JSON y para string numérico.
- Todos los escenarios 400 de la decisión 13, incluidos "" y espacios.
- Bordes de la decisión 15.
- Mensajes distintos para formato inválido y tipo incorrecto.
- Longitud máxima de números en el parser.
- Decisiones: 13, 15, 19. Depende de 1.

### 4. `leads-api-limitar-tamano-cuerpo`
- Filtro propio de 16 KB que cubre Transfer-Encoding chunked; 413 con su `type`.
- Orden 401 > 413 > 400.
- Decisiones: 18 y 20. Depende de 1.

**Riesgo de orden:** entre el change 1 y el 4 el servicio no limita el tamaño
del cuerpo. Si se expone por ngrok antes de cerrar el 4, el riesgo queda
abierto. Hay dos opciones: no exponerlo hasta el 4, o adelantar el 4 al segundo
lugar (es independiente de 2 y 3).

## Decisiones transversales (propuestas, a confirmar en el change 1)

### Formato de errores
ProblemDetail (RFC 9457), `application/problem+json`, desde un único
`@RestControllerAdvice`. Los filtros (clave y tamaño) no escriben la respuesta
por su cuenta: delegan en `HandlerExceptionResolver`, así el advice sigue siendo
el único lugar que arma errores.

| Estado | `type` | Cuándo |
| --- | --- | --- |
| 400 | `validacion` | Campos con errores. Incluye `errores: [{campo, codigo, mensaje}]` |
| 400 | `json_invalido` | Cuerpo que no es JSON parseable, o número que excede la longitud máxima |
| 401 | `no_autorizado` | Sin X-API-Key o con una clave incorrecta |
| 404 | `no_encontrado` | Ruta desconocida (con clave válida) |
| 405 | `metodo_no_permitido` | Método no soportado (con clave válida) |
| 413 | `cuerpo_demasiado_grande` | Cuerpo de más de 16 KB |
| 500 | `error_interno` | Error no previsto, sin detalle interno |

Códigos de `errores[].codigo`: `requerido`, `tipo_invalido`, `formato_invalido`,
`valor_no_permitido`, `fuera_de_rango`. Falta decidir la forma final de las URIs
de `type`.

### Paquetes
Primero por funcionalidad y después por capa, bajo `com.sagant.leads.leads_api`:

```
evaluacion/
  api/        controller, DTO de request y response
  dominio/    solicitud validada, resultado, motor
  reglas/     ReglaExcluyente, ReglaPuntaje y una clase por regla
  config/     propiedades de reglas, validación de coherencia, version_reglas
seguridad/    filtro de clave, propiedades
comun/
  error/      advice, tipos de error
  web/        filtro de tamaño del cuerpo
```

### Logging
Una línea por solicitud con método, ruta, estado y duración. Nunca el cuerpo,
el header X-API-Key ni los campos extra. Los ejemplos y fixtures de test usan
datos ficticios.

### Configuración
- `leads.reglas.*`: `@ConfigurationProperties` con `@Validated`. Umbrales y
  puntajes con BigDecimal donde corresponde. La coherencia (ALTA > MEDIA,
  0,3 < 0,5, etc.) se valida al arrancar.
- `leads.seguridad.api-key: ${LEADS_API_KEY}` con `@NotBlank`. Los tests usan
  una clave ficticia.
- `spring.jackson.factory.constraints.read.max-number-length` (ver notas,
  punto a).

## Preguntas abiertas por change

### Change 1
- ¿Clave de API con un filtro propio o con Spring Security? Recomendación:
  filtro propio, sin agregar dependencias.
- ¿Forma de las URIs de `type`: `urn:leads-api:error:<caso>` o una URL?
- ¿Se loguea `session_id`? No identifica a la persona, pero permite
  correlacionar con Google Sheets.
- ¿Largo mínimo de `LEADS_API_KEY` al arrancar?
- ¿El Content-Type de la respuesta declara `charset=UTF-8` o queda sin
  parámetro (RFC 8259)?
- ¿Se mantiene lombok si los DTO y la configuración son records?
- **A confirmar con negocio:** con `clearing = si` ninguna solicitud llega a
  ALTA (máximo 55). El máximo de estudiante (60) cae justo en el borde de ALTA.

### Change 2
- ¿El motivo "Score ajustado a 0 por inelegibilidad (suma X)" aparece también
  cuando la suma ya es 0 o está entre 0 y 100? La decisión 3 implica que sí,
  siempre que sea inelegible; falta confirmarlo.

### Change 3
- Claves duplicadas en el JSON: ¿400 o gana la última? Se puede activar
  `spring.jackson.read.strict-duplicate-detection`.
- Un string numérico negativo ("-5"): ¿se informa como `formato_invalido` o
  como `fuera_de_rango`?

### Change 4
- ¿Un Content-Length mayor a 16 KB se rechaza antes de leer? Recomendación:
  sí, y además contar lo leído.
- Borde exacto: 16384 bytes pasa y 16385 da 413. Falta confirmarlo.

## Próximo paso
Crear `change/leads-api-crear-evaluacion-puntaje` y correr
`/opsx:propose leads-api-crear-evaluacion-puntaje`.
