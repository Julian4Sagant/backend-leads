# acceso-api Specification

## Purpose

Controlar el acceso al servicio expuesto a internet y definir cómo responde y
registra los errores: clave de API obligatoria, endpoints expuestos, formato y
catálogo de errores, y qué datos pueden aparecer en los logs.

## Convenciones

- **Clave válida:** el valor configurado en `LEADS_API_KEY`. Los tests usan
  una clave ficticia de 32 caracteres o más.
- **Solicitud base:** la definida en la spec `evaluacion-leads`.
- "error `<caso>`" significa una respuesta con `type`
  `urn:leads-api:error:<caso>`.

## Requirements

### Requirement: Clave de API obligatoria
Toda petición, a cualquier ruta y con cualquier método, SHALL llevar el header
X-API-Key con la clave válida. Sin header o con una clave distinta, el sistema
MUST responder 401 con el error `no-autorizado`, sin leer el cuerpo. La
comparación de la clave MUST ser en tiempo constante.

#### Scenario: Clave válida
- **WHEN** se envía POST /evaluar con la solicitud base y la clave válida
- **THEN** responde 200

#### Scenario: Sin clave
- **WHEN** se envía POST /evaluar con la solicitud base sin el header X-API-Key
- **THEN** responde 401 con el error `no-autorizado`

#### Scenario: Clave incorrecta
- **WHEN** se envía POST /evaluar con la solicitud base y una clave distinta de la válida
- **THEN** responde 401 con el error `no-autorizado`

#### Scenario: Clave con un carácter de menos
- **WHEN** se envía POST /evaluar con la solicitud base y la clave válida sin su último carácter
- **THEN** responde 401 con el error `no-autorizado`

#### Scenario: Sin clave y cuerpo inválido
- **WHEN** se envía POST /evaluar sin el header X-API-Key y con el cuerpo `{"edad":`
- **THEN** responde 401 con el error `no-autorizado`, no 400

### Requirement: Rutas y métodos no soportados
Con la clave válida, una ruta desconocida SHALL responder 404 con el error
`no-encontrado` y un método no soportado en /evaluar, 405 con el error
`metodo-no-permitido`. Sin clave, ambos casos MUST responder 401.

#### Scenario: Ruta desconocida con clave
- **WHEN** se envía GET /no-existe con la clave válida
- **THEN** responde 404 con el error `no-encontrado`

#### Scenario: Ruta desconocida sin clave
- **WHEN** se envía GET /no-existe sin el header X-API-Key
- **THEN** responde 401 con el error `no-autorizado`

#### Scenario: Método no soportado con clave
- **WHEN** se envía GET /evaluar con la clave válida
- **THEN** responde 405 con el error `metodo-no-permitido`

#### Scenario: Método no soportado sin clave
- **WHEN** se envía GET /evaluar sin el header X-API-Key
- **THEN** responde 401 con el error `no-autorizado`

### Requirement: Sin endpoints de diagnóstico ni documentación
El servicio MUST NOT exponer endpoints de diagnóstico (actuator) ni de
documentación de la API (springdoc, Swagger UI). Con la clave válida, esas
rutas SHALL responder 404.

#### Scenario: Health no expuesto
- **WHEN** se envía GET /actuator/health con la clave válida
- **THEN** responde 404 con el error `no-encontrado`

#### Scenario: Documentación OpenAPI no expuesta
- **WHEN** se envía GET /v3/api-docs con la clave válida
- **THEN** responde 404 con el error `no-encontrado`

#### Scenario: Swagger UI no expuesto
- **WHEN** se envía GET /swagger-ui.html con la clave válida
- **THEN** responde 404 con el error `no-encontrado`

### Requirement: Clave configurada al arrancar
La aplicación MUST NOT arrancar si `LEADS_API_KEY` falta, tiene menos de 32
caracteres, o contiene espacios o caracteres fuera de ASCII imprimible. El
mensaje de error SHALL indicar la condición incumplida sin mostrar la clave.

#### Scenario: Sin clave configurada
- **WHEN** la aplicación arranca sin `LEADS_API_KEY`
- **THEN** la aplicación no arranca y POST /evaluar no recibe respuesta

#### Scenario: Clave de 31 caracteres
- **WHEN** la aplicación arranca con una `LEADS_API_KEY` de 31 caracteres
- **THEN** la aplicación no arranca y POST /evaluar no recibe respuesta

#### Scenario: Clave de 32 caracteres
- **WHEN** la aplicación arranca con una `LEADS_API_KEY` de 32 caracteres ASCII imprimibles y se envía POST /evaluar con la solicitud base y esa clave
- **THEN** responde 200

#### Scenario: Clave con espacio
- **WHEN** la aplicación arranca con una `LEADS_API_KEY` de 40 caracteres que incluye un espacio
- **THEN** la aplicación no arranca y POST /evaluar no recibe respuesta

#### Scenario: Clave con carácter no ASCII
- **WHEN** la aplicación arranca con una `LEADS_API_KEY` de 40 caracteres que incluye "ñ"
- **THEN** la aplicación no arranca y POST /evaluar no recibe respuesta

#### Scenario: Mensaje de arranque sin la clave
- **WHEN** la aplicación arranca con una `LEADS_API_KEY` de 31 caracteres
- **THEN** el mensaje de error menciona el mínimo de 32 caracteres y no contiene la clave

### Requirement: Formato de las respuestas de error
Toda respuesta de error SHALL ser ProblemDetail (RFC 9457) con Content-Type
`application/problem+json;charset=UTF-8` y los campos `type`, `title` y
`status`. `type` MUST ser una URN del catálogo `urn:leads-api:error:<caso>` y
nunca `about:blank`.

#### Scenario: Error con ProblemDetail
- **WHEN** se envía POST /evaluar con la solicitud base sin el header X-API-Key
- **THEN** responde 401 con Content-Type `application/problem+json;charset=UTF-8`, `type` `urn:leads-api:error:no-autorizado` y `status` 401

#### Scenario: Ningún error con about:blank
- **WHEN** se envía GET /no-existe con la clave válida
- **THEN** responde 404 y `type` es `urn:leads-api:error:no-encontrado`, no `about:blank`

### Requirement: Catálogo de type de error
Cada caso de error SHALL tener un único `type` estable, en minúscula, ASCII y
con guion, con su estado: `validacion` 400, `json-invalido` 400,
`no-autorizado` 401, `no-encontrado` 404, `metodo-no-permitido` 405,
`cuerpo-demasiado-grande` 413, `tipo-de-contenido-no-soportado` 415 e
`interno` 500, todos con el prefijo `urn:leads-api:error:`. Un change que
necesita un caso nuevo lo agrega acá.

#### Scenario: Error interno sin detalles
- **WHEN** un error no previsto ocurre al atender POST /evaluar con la solicitud base
- **THEN** responde 500 con `type` `urn:leads-api:error:interno`, sin mensaje de excepción, clase ni traza en el cuerpo

#### Scenario: Cada type usado está en el catálogo
- **WHEN** se provocan los errores 400, 401, 404, 405, 413, 415 y 500 de los escenarios de esta spec
- **THEN** cada respuesta tiene un `type` del catálogo y su `status` coincide con el del catálogo

### Requirement: Errores de validación
El error `validacion` SHALL incluir `errores`, una lista con al menos un
elemento `{campo, codigo, mensaje}`. `codigo` MUST ser uno de `requerido`,
`tipo-invalido`, `formato-invalido`, `valor-no-permitido` o `fuera-de-rango`.
Los códigos de dominio y los nombres de campo conservan el guion bajo.

#### Scenario: Lista de errores con un elemento
- **WHEN** se envía POST /evaluar con la solicitud base y `edad` 101
- **THEN** responde 400 con `type` `urn:leads-api:error:validacion` y `errores` con un elemento cuyo `campo` es `edad` y `codigo` es `fuera-de-rango`

#### Scenario: Códigos con guion
- **WHEN** se envía POST /evaluar con la solicitud base, `monto` true, `plazo_meses` 12.5, `clearing` "talvez" y `edad` 101, y sin `urgencia`
- **THEN** responde 400 y los `codigo` de `errores` son exactamente `tipo-invalido`, `formato-invalido`, `valor-no-permitido`, `fuera-de-rango` y `requerido`

### Requirement: JSON inválido
Un cuerpo que no se puede leer como JSON (vacío, truncado, texto suelto) o con
un número que supera la longitud máxima del parser SHALL responder 400 con el
error `json-invalido`, sin la lista `errores`. Un objeto JSON válido, aunque
esté vacío, MUST evaluarse como `validacion`.

#### Scenario: JSON truncado
- **WHEN** se envía POST /evaluar con el cuerpo `{"edad": 32,`
- **THEN** responde 400 con el error `json-invalido` y sin `errores`

#### Scenario: Texto suelto
- **WHEN** se envía POST /evaluar con el cuerpo `hola`
- **THEN** responde 400 con el error `json-invalido` y sin `errores`

#### Scenario: Cuerpo vacío
- **WHEN** se envía POST /evaluar sin cuerpo
- **THEN** responde 400 con el error `json-invalido` y sin `errores`

#### Scenario: Número más largo que el límite del parser
- **WHEN** se envía POST /evaluar con la solicitud base y `monto` como número JSON de 1001 dígitos
- **THEN** responde 400 con el error `json-invalido`, sin `errores` y nunca 500

#### Scenario: Objeto vacío es validación
- **WHEN** se envía POST /evaluar con el cuerpo `{}`
- **THEN** responde 400 con el error `validacion`, no `json-invalido`

### Requirement: Tipo de contenido de la solicitud
POST /evaluar SHALL aceptar solo cuerpos con Content-Type `application/json`,
con o sin parámetro `charset`. Cualquier otro Content-Type, o su ausencia,
MUST responder 415 con el error `tipo-de-contenido-no-soportado`. Sin clave
válida, la respuesta MUST ser 401.

#### Scenario: Content-Type JSON con charset
- **WHEN** se envía POST /evaluar con la solicitud base y Content-Type `application/json;charset=UTF-8`
- **THEN** responde 200

#### Scenario: Sin Content-Type
- **WHEN** se envía POST /evaluar con la solicitud base sin header Content-Type
- **THEN** responde 415 con el error `tipo-de-contenido-no-soportado`

#### Scenario: Content-Type de texto plano
- **WHEN** se envía POST /evaluar con la solicitud base y Content-Type `text/plain`
- **THEN** responde 415 con el error `tipo-de-contenido-no-soportado`

#### Scenario: Content-Type incorrecto sin clave
- **WHEN** se envía POST /evaluar con la solicitud base, Content-Type `text/plain` y sin el header X-API-Key
- **THEN** responde 401 con el error `no-autorizado`

### Requirement: Respuesta siempre en JSON
El sistema SHALL responder siempre JSON, sin importar el header Accept. Nunca
MUST responder 406.

#### Scenario: Accept de HTML en una evaluación
- **WHEN** se envía POST /evaluar con la solicitud base y Accept `text/html`
- **THEN** responde 200 con Content-Type `application/json;charset=UTF-8` y el mismo cuerpo que sin Accept

#### Scenario: Accept de HTML en un error
- **WHEN** se envía POST /evaluar con la solicitud base, `edad` 101 y Accept `text/html`
- **THEN** responde 400 con Content-Type `application/problem+json;charset=UTF-8` y el error `validacion`

### Requirement: Charset de las respuestas
Las respuestas exitosas SHALL declarar `application/json;charset=UTF-8` y las
de error `application/problem+json;charset=UTF-8`, y su cuerpo MUST estar
codificado en UTF-8.

#### Scenario: Charset en respuesta exitosa
- **WHEN** se envía POST /evaluar con la solicitud base
- **THEN** responde 200 con Content-Type `application/json;charset=UTF-8`

#### Scenario: Charset en respuesta de error
- **WHEN** se envía POST /evaluar con la solicitud base y `monto` "abc"
- **THEN** responde 400 con Content-Type `application/problem+json;charset=UTF-8` y el cuerpo decodificado como UTF-8 contiene el `mensaje` del error

### Requirement: Registro de evaluaciones
Cada evaluación exitosa SHALL dejar una única línea de log con `session_id`,
`prioridad`, `score` y `version_reglas`, y ningún otro dato de la solicitud.

#### Scenario: Línea de evaluación exitosa
- **WHEN** se envía POST /evaluar con la solicitud base
- **THEN** responde 200 y el log tiene una línea con `beb55e7eb0c947ff86f19aa0e0d0b2db`, `ALTA`, `85` y el `version_reglas` de la respuesta

#### Scenario: Sin otros datos de la solicitud
- **WHEN** se envía POST /evaluar con la solicitud base, `monto` 123457.89, `ingreso_mensual` 98765.43 y `cuota_deudas` 4321.09
- **THEN** responde 200 y el log no contiene `123457`, `98765` ni `4321`

### Requirement: Registro de errores
Cada respuesta de error SHALL dejar una línea de log con el `type` y el
estado, más el `session_id` si se leyó y cumple su formato. El log MUST NOT
contener valores recibidos, mensajes de error de campo, la clave ni el
contenido de X-API-Key.

#### Scenario: Error de validación con session_id válido
- **WHEN** se envía POST /evaluar con la solicitud base y `edad` 101
- **THEN** responde 400 y el log tiene una línea con `urn:leads-api:error:validacion`, `400` y `beb55e7eb0c947ff86f19aa0e0d0b2db`

#### Scenario: Error de validación sin valores del cuerpo
- **WHEN** se envía POST /evaluar con la solicitud base, `monto` "valor-secreto-123" y `motivo` "otro-secreto-456"
- **THEN** responde 400 y el log no contiene `valor-secreto-123`, `otro-secreto-456` ni el `mensaje` de ningún error de campo

#### Scenario: session_id con salto de línea no se registra
- **WHEN** se envía POST /evaluar con la solicitud base y `session_id` "abc\ndef"
- **THEN** responde 400 y el log no contiene `abc` ni `def` y no tiene una línea extra

#### Scenario: JSON inválido sin session_id
- **WHEN** se envía POST /evaluar con el cuerpo `{"session_id": "beb55e7eb0c947ff86f19aa0e0d0b2db",`
- **THEN** responde 400 con el error `json-invalido` y la línea de log tiene el `type` y `400` pero no el `session_id`

#### Scenario: Clave incorrecta no se registra
- **WHEN** se envía POST /evaluar con la solicitud base y la clave "clave-incorrecta-de-prueba-0000000000"
- **THEN** responde 401 y el log tiene una línea con `urn:leads-api:error:no-autorizado` y `401` pero no contiene `clave-incorrecta-de-prueba-0000000000` ni la clave válida

### Requirement: Tamaño máximo del cuerpo
El cuerpo de toda petición SHALL tener como máximo 16384 bytes, contados sin
los headers. Un cuerpo mayor MUST responder 413 con el error
`cuerpo-demasiado-grande` y sin `errores`, tanto si declara su tamaño con
Content-Length como si lo envía sin declararlo (Transfer-Encoding chunked).
Un cuerpo de 16384 bytes o menos SHALL procesarse como cualquier otro. El 413
deja su línea de log de error sin `session_id`.

#### Scenario: Cuerpo de 16384 bytes
- **WHEN** se envía POST /evaluar con la clave válida y la solicitud base más un campo extra de relleno, con Content-Length y 16384 bytes en total
- **THEN** responde 200 con la misma `prioridad`, `score` y `motivos` que la solicitud base

#### Scenario: Cuerpo de 16385 bytes
- **WHEN** se envía POST /evaluar con la clave válida y la solicitud base más un campo extra de relleno, con Content-Length y 16385 bytes en total
- **THEN** responde 413 con Content-Type `application/problem+json;charset=UTF-8`, `type` `urn:leads-api:error:cuerpo-demasiado-grande`, `status` 413 y sin `errores`

#### Scenario: Cuerpo grande sin tamaño declarado
- **WHEN** se envía POST /evaluar con la clave válida y un cuerpo de 20480 bytes con Transfer-Encoding chunked, sin Content-Length
- **THEN** responde 413 con el error `cuerpo-demasiado-grande`

#### Scenario: Cuerpo chico sin tamaño declarado
- **WHEN** se envía POST /evaluar con la clave válida y la solicitud base con Transfer-Encoding chunked, sin Content-Length
- **THEN** responde 200 con `prioridad` "ALTA" y `score` 85

#### Scenario: Log del cuerpo demasiado grande
- **WHEN** se envía POST /evaluar con la clave válida y la solicitud base con `session_id` "beb55e7eb0c947ff86f19aa0e0d0b2db" más un campo extra de relleno, con Content-Length y 16385 bytes en total
- **THEN** responde 413 y el log tiene una línea con `urn:leads-api:error:cuerpo-demasiado-grande` y `413`, sin `beb55e7eb0c947ff86f19aa0e0d0b2db` ni el contenido del relleno

### Requirement: Orden del límite de tamaño
Sin clave válida, una petición con un cuerpo de más de 16384 bytes MUST
responder 401, sin leer el cuerpo. Con clave válida, un Content-Length mayor
que el límite SHALL responder 413 antes de controlar la ruta, el método, el
Content-Type y el JSON. Sin Content-Length, el límite se controla al leer el
cuerpo, así que una ruta que no lo lee responde como siempre.

#### Scenario: Cuerpo grande sin clave
- **WHEN** se envía POST /evaluar sin el header X-API-Key, con Content-Length y un cuerpo de 20480 bytes
- **THEN** responde 401 con el error `no-autorizado`, no 413

#### Scenario: Cuerpo grande con JSON inválido
- **WHEN** se envía POST /evaluar con la clave válida, con Content-Length y un cuerpo de 20480 bytes que no es JSON válido
- **THEN** responde 413 con el error `cuerpo-demasiado-grande`, no 400

#### Scenario: Cuerpo grande sin tamaño declarado y JSON inválido
- **WHEN** se envía POST /evaluar con la clave válida y un cuerpo de 20480 bytes que no es JSON válido, con Transfer-Encoding chunked y sin Content-Length
- **THEN** responde 413 con el error `cuerpo-demasiado-grande`, no 400

#### Scenario: Cuerpo grande a una ruta desconocida
- **WHEN** se envía POST /no-existe con la clave válida, con Content-Length y un cuerpo de 20480 bytes
- **THEN** responde 413 con el error `cuerpo-demasiado-grande`, no 404

#### Scenario: Cuerpo grande con método no soportado
- **WHEN** se envía PUT /evaluar con la clave válida, con Content-Length y un cuerpo de 20480 bytes
- **THEN** responde 413 con el error `cuerpo-demasiado-grande`, no 405

#### Scenario: Cuerpo grande con Content-Type de texto plano
- **WHEN** se envía POST /evaluar con la clave válida, Content-Type `text/plain`, Content-Length y un cuerpo de 20480 bytes
- **THEN** responde 413 con el error `cuerpo-demasiado-grande`, no 415

#### Scenario: Cuerpo grande sin tamaño declarado a una ruta desconocida
- **WHEN** se envía POST /no-existe con la clave válida y un cuerpo de 20480 bytes con Transfer-Encoding chunked, sin Content-Length
- **THEN** responde 404 con el error `no-encontrado`
