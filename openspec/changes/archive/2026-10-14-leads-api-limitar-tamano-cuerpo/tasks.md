# Tasks

Convención: los escenarios se nombran entre comillas angulares, exactamente
como en la spec delta. Los tests de escenario llevan `@DisplayName` con ese
nombre. Cada grupo termina con `./mvnw test` en verde.

Este change no toca los motivos: el test que verifica su orden completo
(`OrdenMotivosTest`) no cambia y tiene que seguir pasando en cada grupo.

## 1. Límite del tamaño declarado

- [x] 1.1 Crear `CuerpoDemasiadoGrandeException` en `comun/exception` (D2) y su `@ExceptionHandler` en `ManejadorErrores`, que responde `CUERPO_DEMASIADO_GRANDE` (D3); verificar con un test unitario del manejador que da 413 con el `type` del catálogo y sin `errores`
- [x] 1.2 Crear `FiltroTamanoCuerpo` en `seguridad/filter` con `@Order(Ordered.HIGHEST_PRECEDENCE + 1)` y `MAXIMO_BYTES = 16384`, que con Content-Length mayor al límite delega la excepción en el `HandlerExceptionResolver` sin seguir la cadena (D1, D2); verificar con un test unitario que 16384 sigue la cadena y 16385 no
- [x] 1.3 Tests HTTP de «Cuerpo de 16384 bytes» y «Cuerpo de 16385 bytes», con el relleno armado en bytes UTF-8 exactos (D5); verificar el Content-Type `application/problem+json;charset=UTF-8`, el `type`, el `status` y la ausencia de `errores`
- [x] 1.4 Tests HTTP de «Cuerpo grande sin clave», «Cuerpo grande con JSON inválido», «Cuerpo grande a una ruta desconocida», «Cuerpo grande con método no soportado» y «Cuerpo grande con Content-Type de texto plano»; verificar cada código de estado y que ninguno es el de la alternativa (400, 404, 405, 415)
- [x] 1.5 Test de «Log del cuerpo demasiado grande» con la captura de salida, como en `LogsHttpTest`; verificar que la línea tiene el `type` y `413` y no tiene el `session_id` ni el relleno
- [x] 1.6 Actualizar el test de «Cada type usado está en el catálogo» para que también provoque el 413; verificar que su `type` y su `status` coinciden con el catálogo

## 2. Límite al leer el cuerpo (chunked)

- [x] 2.1 Sumar al filtro el `HttpServletRequestWrapper` con un `ServletInputStream` que cuenta los bytes de `read()` y `read(byte[], int, int)` y lanza la excepción al pasar 16384, con `getReader()` sobre el mismo stream (D2); verificar con un test unitario con un request sin Content-Length que 16384 bytes se leen completos y el byte 16385 lanza la excepción
- [x] 2.2 Hacer que `ManejadorErrores` recorra la cadena de causas de `HttpMessageNotReadableException` y responda 413 si encuentra `CuerpoDemasiadoGrandeException`, y `json-invalido` si no (D3); verificar que los tests de JSON inválido existentes siguen dando 400
- [x] 2.3 Agregar `FiltroTamanoCuerpo.leerElResto(request)`, usarlo en `ManejadorErrores` ante un JSON ilegible y en el `RequestBodyAdvice` `LimiteCuerpoLeido` después de leer el cuerpo (D3, lectura del resto); verificar con tests unitarios que un cuerpo ya empezado y de más de 16384 bytes da 413 y que uno sin empezar no se lee
- [x] 2.4 Tests con servidor real y cliente HTTP con cuerpo de largo desconocido (D5) de «Cuerpo grande sin tamaño declarado», «Cuerpo chico sin tamaño declarado», «Cuerpo grande sin tamaño declarado y JSON inválido» y «Cuerpo grande sin tamaño declarado a una ruta desconocida», más un JSON válido chico seguido de 20 KB de relleno (413); verificar en cada uno que el pedido no lleva Content-Length
- [x] 2.5 Verificar con el script de cobertura que cada escenario de las specs principales y del delta tiene un test con su `@DisplayName`, sin nombres huérfanos
- [x] 2.6 Actualizar `docs/notas-tecnicas-leads.md` (puntos b y d: paquete `seguridad/filter`, los dos controles y la lectura del resto), `docs/integracion-n8n.md` (fila del 413 en "Cómo leer la planilla" y sin la advertencia del tamaño del cuerpo) y `docs/roadmap.md` (change 4 implementado y "Riesgo de orden" cerrado); verificar que no contradicen la spec

## 3. Cierre

- [x] 3.1 Correr `./mvnw clean test` completo; verificar que todos los tests pasan
- [x] 3.2 Correr `openspec validate leads-api-limitar-tamano-cuerpo --strict`; verificar que pasa sin advertencias
- [x] 3.3 Revisión del código por una persona en el PR; verificar que no quedan comentarios sin resolver

## Workflow follow-up

- `/opsx:archive` como último commit del branch, con el PR revisado y los tests en verde.
- Después del archive, comprobar `openspec/specs/acceso-api/spec.md`.
- Después del merge, reiniciar el servicio.
