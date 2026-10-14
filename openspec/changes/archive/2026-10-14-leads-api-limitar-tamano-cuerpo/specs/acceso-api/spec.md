# Spec Delta

## ADDED Requirements

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

## MODIFIED Requirements

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
