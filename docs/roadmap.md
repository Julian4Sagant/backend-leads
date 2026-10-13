# Roadmap de leads-api

Plan corto de changes para construir leads-api, el servicio sin estado que
recibe una solicitud de préstamo ya normalizada por n8n y devuelve la prioridad
calculada por el motor de reglas. Surge del explore inicial (2026-10-09) y se
actualizó con el explore del change 1 (2026-10-13), que eligió Drools como
motor de reglas. Las verificaciones técnicas están en
[notas-tecnicas-leads.md](notas-tecnicas-leads.md).

Estado: change 1 en curso (`change/leads-api-crear-evaluacion-puntaje`).
`openspec/specs/` está vacío.

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
- Clave de API con un filtro propio, sin Spring Security (401; la app no
  arranca sin `LEADS_API_KEY` o con una clave de menos de 32 caracteres, con
  caracteres no ASCII imprimibles o con espacios), sin actuator ni springdoc,
  404/405 con clave válida.
- ProblemDetail desde un único `@RestControllerAdvice`, con la tabla de `type`
  y `charset=UTF-8` explícito.
- Drools 10.2.0 como motor de reglas (ver "Motor de reglas").
- Se saca lombok del `pom.xml`.
- Contrato nuevo. **Cambio incompatible para n8n**, a declarar en "Contratos
  afectados": sale `nombre` del request; entran `edad` y `antiguedad_meses`
  (requeridos); entra `version_reglas` en la response.
- Validación: requeridos, valores permitidos de cada código, rangos, todos los
  errores juntos, campos extra ignorados.
- Las 7 reglas de puntaje en el DRL (motivo `PUNTAJE`), prioridad, recorte
  0/100 con su motivo, orden de motivos, carga con BigDecimal sin dividir.
- Puntajes y umbrales en `application.yml` con validación de coherencia;
  `version_reglas` y sus tests; tabla de máximos y su test; UTF-8.
- Decisiones: 1, 2, 4 a 12, 14, 16, 17, 21 a 24.
- El DTO usa desde el inicio la representación "cruda" de los números (notas,
  punto c), para que el change 3 no tenga que rehacerlo.

### 2. `leads-api-agregar-elegibilidad-edad`
- Regla de edad en el DRL (motivo `ELEGIBILIDAD`; < 18: BAJA, score 0).
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

## Decisiones transversales (confirmadas en el explore del change 1)

### Motor de reglas
Drools 10.2.0, verificado con un spike (notas técnicas, "Spike de Drools").

```
 POST /evaluar -> filtro de clave -> validación -> Solicitud tipada
                                                       |
                                                       v
                  StatelessKieSession (una por solicitud)
                  hechos: Solicitud, Resultado; global: puntajes
                  cada regla emite un código de motivo con su posición
                                                       |
                                                       v
                  Java: textos, orden por posición, suma, recorte o
                  inelegibilidad, score, prioridad
```

- Cada regla de negocio es una regla del DRL con un identificador estable.
  Excluyentes y de puntaje se distinguen por el tipo de motivo que emiten
  (`ELEGIBILIDAD` o `PUNTAJE`), no por interfaces de Java.
- El DRL emite códigos y es solo ASCII. Los textos en español se arman en Java.
- Los puntajes y umbrales siguen en `application.yml` y llegan al DRL como
  global. El DRL no lleva números de puntaje.
- El DRL se compila al arrancar. Si tiene errores, la app no arranca.
- `version_reglas` = SHA-256 de (configuración canónica + DRL con finales de
  línea LF + constante de revisión), truncado a 12 caracteres hexadecimales.
  La constante se sube a mano cuando cambia la lógica en Java o un texto. El
  test de cierre vigila la lista de reglas del `KieBase` compilado.

### Formato de errores
ProblemDetail (RFC 9457), `application/problem+json;charset=UTF-8`, desde un
único `@RestControllerAdvice`. Los filtros (clave y tamaño) no escriben la
respuesta por su cuenta: delegan en `HandlerExceptionResolver`, así el advice
sigue siendo el único lugar que arma errores.

Los `type` son URN `urn:leads-api:error:<caso>`, en minúsculas, ASCII y con
guion. Uno por caso, estable y registrado en la spec. Un change que necesita un
`type` nuevo lo agrega a la spec en ese change. Nunca se usa `about:blank`.

| Estado | `type` | Cuándo |
| --- | --- | --- |
| 400 | `urn:leads-api:error:validacion` | Campos con errores. Siempre trae al menos un error en `errores: [{campo, codigo, mensaje}]` |
| 400 | `urn:leads-api:error:json-invalido` | Cuerpo que no es JSON válido, texto suelto, cuerpo vacío, o número que excede la longitud máxima del parser. Nunca trae lista de errores |
| 401 | `urn:leads-api:error:no-autorizado` | Sin X-API-Key o con una clave incorrecta |
| 404 | `urn:leads-api:error:no-encontrado` | Ruta desconocida (con clave válida) |
| 405 | `urn:leads-api:error:metodo-no-permitido` | Método no soportado (con clave válida) |
| 413 | `urn:leads-api:error:cuerpo-demasiado-grande` | Cuerpo de más de 16 KB |
| 500 | `urn:leads-api:error:interno` | Error no previsto, sin detalles internos en la respuesta |

`{}` es JSON válido: va a `validacion`, con un error `requerido` por campo.

Códigos de `errores[].codigo`: `requerido`, `tipo-invalido`, `formato-invalido`,
`valor-no-permitido`, `fuera-de-rango`. El guion es solo para identificadores
de error. Los códigos de dominio (`esta_semana`, `1_a_3_meses`, `no_se`) y los
nombres de campo (`session_id`, `plazo_meses`) siguen con guion bajo porque son
el contrato con n8n y el Sheet.

### Paquetes
Primero por funcionalidad y después por capa, bajo `com.sagant.leads.leads_api`:

```
evaluacion/
  api/        controller, DTO de request y response
  dominio/    solicitud validada, resultado, motivos, cálculo del score
  reglas/     carga del DRL, sesión de Drools, textos de los motivos
  config/     propiedades de reglas, validación de coherencia, version_reglas
seguridad/    filtro de clave, propiedades
comun/
  error/      advice, tipos de error
  web/        filtro de tamaño del cuerpo
```

El DRL vive en `src/main/resources/reglas/`.

### Logging
- Una línea por evaluación exitosa con `session_id`, prioridad, score y
  `version_reglas`.
- Una línea por respuesta de error con el `type` y el estado, más el
  `session_id` si es válido. En `json-invalido`, 401 y 413 la línea va sin
  `session_id`.
- `session_id` se loguea solo si cumple `^[A-Za-z0-9_-]{1,64}$`, y nunca junto
  a ningún otro campo de la solicitud.
- Nunca se loguean el cuerpo, los valores recibidos, los mensajes de los
  errores de campo, el header X-API-Key ni los campos extra.
- Los ejemplos y fixtures de test usan datos ficticios.

### Configuración
- `leads.reglas.*`: `@ConfigurationProperties` con `@Validated`. Umbrales y
  puntajes con BigDecimal donde corresponde. La coherencia (ALTA > MEDIA,
  0,3 < 0,5, etc.) se valida al arrancar.
- `leads.seguridad.api-key: ${LEADS_API_KEY}`: mínimo 32 caracteres ASCII
  imprimibles y sin espacios. Si no cumple, la app no arranca y el mensaje no
  muestra la clave. Sugerencia para generarla: `openssl rand -base64 32`. Los
  tests usan una clave ficticia.
- `spring.jackson.factory.constraints.read.max-number-length` (ver notas,
  punto a).

### Comportamiento de negocio confirmado
Con los puntajes actuales (tabla en las notas técnicas, punto f):
- con `clearing = si` ninguna solicitud llega a ALTA (máximo 55);
- un estudiante llega a ALTA solo con el puntaje perfecto (60, justo en el
  borde);
- un desempleado nunca llega a ALTA (máximo 50).

## Preguntas abiertas por change

### Change 1
Ninguna: todas se cerraron en el explore del 2026-10-13.

### Change 2
- ¿El motivo "Score ajustado a 0 por inelegibilidad (suma X)" aparece también
  cuando la suma ya es 0 o está entre 0 y 100? La decisión 3 implica que sí,
  siempre que sea inelegible; falta confirmarlo.

### Change 3
- Claves duplicadas en el JSON: ¿400 o gana la última? Se puede activar
  `spring.jackson.read.strict-duplicate-detection`.
- Un string numérico negativo ("-5"): ¿se informa como `formato-invalido` o
  como `fuera-de-rango`?

### Change 4
- ¿Un Content-Length mayor a 16 KB se rechaza antes de leer? Recomendación:
  sí, y además contar lo leído.
- Borde exacto: 16384 bytes pasa y 16385 da 413. Falta confirmarlo.

## Próximo paso
Correr `/opsx:propose leads-api-crear-evaluacion-puntaje` en el branch
`change/leads-api-crear-evaluacion-puntaje`.
