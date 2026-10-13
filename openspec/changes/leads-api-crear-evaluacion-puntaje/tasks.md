# Tasks

Convención: los escenarios se nombran entre comillas angulares, exactamente
como en las specs. Las tareas de test HTTP usan MockMvc con una clave ficticia
y leen la respuesta como bytes UTF-8. Cada grupo termina con `./mvnw test` en
verde.

## 1. Base del proyecto y build

- [x] 1.1 Sacar lombok y su `annotationProcessorPaths` de las dos ejecuciones del compilador en `pom.xml`; verificar que `./mvnw -q dependency:tree` no lista lombok
- [x] 1.2 Importar `org.drools:drools-bom:10.2.0` y agregar `drools-engine` y `drools-mvel`; verificar con `./mvnw dependency:tree` que no entra `com.fasterxml.jackson.core:jackson-databind`
- [x] 1.3 Reemplazar `application.properties` por `application.yml` con la configuración de D7 y agregar una clave ficticia de test (40 caracteres) en la configuración de test; verificar que `contextLoads` pasa con `./mvnw test`

## 2. Configuración de reglas

- [x] 2.1 Crear los records de `leads.reglas` (prioridad, umbrales, puntajes) con `BigDecimal`, `@Validated` y la validación de coherencia de D7; verificar con un test de arranque que la configuración por defecto se carga
- [x] 2.2 Tests de arranque con `ApplicationContextRunner` de «Umbral de ALTA no mayor que el de MEDIA», «Umbrales de carga invertidos» y «Puntaje faltante»; verificar que el contexto falla y el mensaje nombra la propiedad
- [x] 2.3 Test de tabla de casos de la coherencia con el borde exacto de cada condición (ALTA = MEDIA, carga baja = alta, meses medio = alto, puntaje no entero como 20.5); verificar que cada caso falla o arranca según corresponde

## 3. Motor de reglas con Drools

- [x] 3.1 Crear `Solicitud` (record con enums de códigos y mapeo explícito a los valores del contrato), `Resultado`, `Motivo` y `cargaNoSupera(BigDecimal)`; verificar con un test de tabla el mapeo de cada código y `cargaNoSupera` en 0,3 y 0,5 exactos
- [x] 3.2 Crear el bean `KieBase` de D1 y escribir primero las 3 reglas de carga del DRL con el global `umbrales` en la restricción; verificar con un test de tabla los bordes de carga (exactamente 0,3; 0,3 + 0,01 de cuota; exactamente 0,5; 0,5 + 0,01 de cuota; ingreso 0). Si la restricción con global falla, aplicar la mitigación del design y anotarla en design.md
- [x] 3.3 Escribir las reglas DRL de urgencia, documentación, destino, situación laboral, antigüedad y clearing con sus `@regla`; verificar con una tabla de casos por regla, incluidos los bordes de antigüedad 5, 6, 11 y 12, que cada tramo emite su código y sus puntos
- [x] 3.4 Test "exactamente un motivo por regla de negocio" sobre todas las combinaciones de códigos y tramos; verificar que cada solicitud produce 7 motivos con 7 `@regla` distintas
- [x] 3.5 Test de que el DRL no tiene bytes fuera de ASCII ni números de puntaje o umbral; verificar que falla al agregar "ñ" o un número a una copia de prueba
- [x] 3.6 Crear los textos de motivos de D3 y el formato `+N`/`-N`/`0`; verificar con una tabla de casos que cada `(regla, codigo, puntos)` da el texto exacto de la spec y que ningún texto contiene U+2212
- [x] 3.7 Crear el cálculo de D5 (verificación de un motivo por regla, orden, suma, recorte con su motivo y prioridad); verificar con una tabla de casos sumas -55, 0, 100 y 105 y scores 60, 59, 30 y 29 con configuración de prueba
- [x] 3.8 Test que verifica el orden completo de los motivos y que el motivo final de recorte aparece una sola vez y al final; verificar que sin recorte hay 7 motivos y con recorte 8, con una suma negativa y una suma mayor que 100

## 4. version_reglas

- [x] 4.1 Implementar el cálculo de D8 al arrancar; verificar con tests unitarios que da 12 hex en minúscula, que es igual con la misma entrada, que cambia con un puntaje distinto o un DRL distinto, que 20 y 20.0 dan lo mismo y que CRLF y LF dan lo mismo
- [x] 4.2 Test de cierre de D8 sobre la lista de reglas DRL y sus `@regla` del `KieBase`; verificar que falla al agregar una regla de prueba sin subir `REVISION_LOGICA` y pasa al subirla

## 5. Lectura y validación de la solicitud

- [ ] 5.1 Crear `SolicitudCruda` y su deserializador de D6 (tipo de token y texto original por campo, raíz no objeto, campos extra descartados); verificar con tests unitarios de deserialización que `150000`, `"150000"`, `150000.00`, `true`, `[]`, `null` y un campo ausente se distinguen
- [ ] 5.2 Crear el validador de D6 con los códigos de error y el orden de campo; verificar con una tabla de casos cada `codigo` en cada tipo de campo, incluidos los bordes de rango de la spec y los límites de 12 dígitos enteros y 2 decimales
- [ ] 5.3 Guardar el `session_id` válido en un atributo del request (D11); verificar con un test unitario que un `session_id` con salto de línea, vacío o de 65 caracteres no se guarda

## 6. Endpoint, errores y clave de API

- [ ] 6.1 Crear el record de `leads.seguridad` con la validación de D9 y tests de arranque de «Sin clave configurada», «Clave de 31 caracteres», «Clave con espacio», «Clave con carácter no ASCII» y «Mensaje de arranque sin la clave»; verificar que el contexto falla y el mensaje no contiene la clave
- [ ] 6.2 Crear el controller de POST /evaluar, el `@RestControllerAdvice` de D10 con el catálogo de `type`, el filtro de clave de D9, la negociación que ignora Accept y las líneas de log de D11; verificar con `./mvnw test` que el contexto arranca
- [ ] 6.3 Tests HTTP de la evaluación: «Solicitud base evaluada», «Campo extra ignorado», «Cuerpo que no es un objeto», «Claves de la respuesta exitosa», «Respuesta de error sin claves de evaluación», «Reintento con la misma solicitud», «Reintento de una solicitud inválida» y «Configuración coherente»
- [ ] 6.4 Tests HTTP de campos requeridos y códigos: «Campo ausente», «Campo null», «Objeto vacío», «Varios errores en una misma solicitud», «Código válido», «Código desconocido», «Código con mayúscula», «Código con espacios» y «Código con tipo incorrecto»
- [ ] 6.5 Tests HTTP de formato de números: «Número como string», «Importe con dos decimales», «Importe con doce dígitos enteros», «Importe con trece dígitos enteros», «Importe con tres decimales», «Entero con parte decimal», «Texto no numérico», «String vacío en un número», «Booleano en un número» y «Array en un número»
- [ ] 6.6 Tests HTTP de rangos: «Monto cero», «Monto mínimo positivo», «Ingreso negativo», «Ingreso cero», «Cuota de deudas negativa», «Cuota de deudas cero», «Plazo 0», «Plazo 1», «Plazo 120», «Plazo 121», «Edad 13», «Edad 14», «Edad 100», «Edad 101», «Antigüedad -1», «Antigüedad 0», «Antigüedad 600» y «Antigüedad 601»
- [ ] 6.7 Tests HTTP de `session_id`: «session_id de 64 caracteres», «session_id de 65 caracteres», «session_id vacío», «session_id con carácter no ASCII» y «session_id con salto de línea»
- [ ] 6.8 Tests HTTP de urgencia, documentación y destino: «Urgencia esta semana», «Urgencia este mes», «Urgencia de 1 a 3 meses», «Sin urgencia», «Urgencia inválida no se evalúa», «Con documentación», «Sin documentación», «Documentación inválida», «Destino definido», «Destino no definido» y «Destino inválido»
- [ ] 6.9 Tests HTTP de situación laboral y antigüedad: «Dependiente», «Independiente», «Jubilado», «Estudiante», «Desempleado», «Situación laboral inválida», «Antigüedad de 12 meses», «Antigüedad de 11 meses», «Antigüedad de 6 meses», «Antigüedad de 5 meses», «Antigüedad de independiente», «Antigüedad no aplica a jubilado», «Antigüedad no aplica a desempleado», «Antigüedad no aplica a estudiante» y «Antigüedad que no aplica se valida igual»
- [ ] 6.10 Tests HTTP de clearing y carga: «No figura en el Clearing», «Figura en el Clearing», «No sabe si figura en el Clearing», «Clearing inválido», «Carga exactamente 0,3», «Carga apenas mayor que 0,3», «Carga exactamente 0,5», «Carga apenas mayor que 0,5», «Ingreso cero cae en el tramo más alto» y «Carga sin plazo válido»
- [ ] 6.11 Tests HTTP de score, prioridad y motivos, con configuración de prueba donde la spec lo indica: «Suma negativa», «Suma exactamente 0», «Suma mayor que 100», «Suma exactamente 100», «Solicitud inválida sin score», «Score 60», «Score 59», «Score 30», «Score 29», «Solicitud inválida sin prioridad», «Orden completo de la solicitud base», «Motivo de recorte al final y una sola vez», «Error sin motivos», «Puntaje negativo con guion ASCII», «Caracteres no ASCII en UTF-8» y «Error sin textos de motivos»
- [ ] 6.12 Tests HTTP de la tabla de máximos, recorriendo todas las combinaciones desde la configuración efectiva: «Máximo de dependiente», «Máximo de dependiente con clearing», «Máximo de independiente», «Máximo de independiente con clearing», «Máximo de jubilado», «Máximo de jubilado con clearing», «Máximo de estudiante», «Máximo de estudiante con clearing», «Máximo de desempleado», «Máximo de desempleado con clearing», «Máximo de estudiante en el borde de ALTA», «Máximo con clearing no llega a ALTA» y «Combinación inválida fuera de la tabla»
- [ ] 6.13 Tests HTTP de `version_reglas`, con configuración o DRL de prueba donde la spec lo indica: «Formato de version_reglas», «Misma configuración, misma versión», «Cambio de puntaje cambia la versión», «20 y 20.0 dan la misma versión», «Cambio de reglas cambia la versión», «Finales de línea no cambian la versión» y «Error sin versión»
- [ ] 6.14 Tests HTTP de la clave y las rutas: «Clave válida», «Sin clave», «Clave incorrecta», «Clave con un carácter de menos», «Sin clave y cuerpo inválido», «Clave de 32 caracteres», «Ruta desconocida con clave», «Ruta desconocida sin clave», «Método no soportado con clave», «Método no soportado sin clave», «Health no expuesto», «Documentación OpenAPI no expuesta» y «Swagger UI no expuesto»
- [ ] 6.15 Tests HTTP de errores y contenido: «Error con ProblemDetail», «Ningún error con about:blank», «Error interno sin detalles» (con el servicio de evaluación simulado para que lance una excepción), «Cada type usado está en el catálogo», «Lista de errores con un elemento», «Códigos con guion», «JSON truncado», «Texto suelto», «Cuerpo vacío», «Número más largo que el límite del parser», «Objeto vacío es validación», «Content-Type JSON con charset», «Sin Content-Type», «Content-Type de texto plano», «Content-Type incorrecto sin clave», «Accept de HTML en una evaluación», «Accept de HTML en un error», «Charset en respuesta exitosa» y «Charset en respuesta de error»
- [ ] 6.16 Tests de logs con `OutputCaptureExtension`: «Línea de evaluación exitosa», «Sin otros datos de la solicitud», «Error de validación con session_id válido», «Error de validación sin valores del cuerpo», «session_id con salto de línea no se registra», «JSON inválido sin session_id» y «Clave incorrecta no se registra»

## 7. Documentación

- [ ] 7.1 Actualizar `docs/roadmap.md`: agregar `tipo-de-contenido-no-soportado` (415) a la tabla de `type`, indicar que el header Accept se ignora, mover los bordes de rangos (decisión 15) del change 3 al change 1, anotar en el change 2 que modifica con un MODIFIED el requirement «Orden y forma de los motivos» (8 motivos, 9 con recorte o inelegibilidad) y marcar el change 1 como implementado; verificar que el roadmap no contradice las specs
- [ ] 7.2 Documentar en `docs/notas-tecnicas-leads.md` cómo correr el servicio en desarrollo (`LEADS_API_KEY`, ngrok) y el resultado de la tarea 3.2 sobre restricciones con globals; verificar que los comandos documentados funcionan tal como están escritos

## 8. Cierre

- [ ] 8.1 Correr `./mvnw test` completo; verificar que todos los tests pasan y que la cantidad total coincide con la informada en el grupo anterior más los nuevos
- [ ] 8.2 Correr `openspec validate leads-api-crear-evaluacion-puntaje --strict`; verificar que pasa sin advertencias
- [ ] 8.3 Revisión del código por una persona en el PR (paso 13 del flujo); verificar que no quedan comentarios sin resolver

## Workflow follow-up

- `/opsx:archive` como último commit del branch, con el PR revisado y los tests en verde.
- Después del archive, comprobar `openspec/specs/evaluacion-leads/spec.md` y `openspec/specs/acceso-api/spec.md`, incluida la sección "Convenciones".
