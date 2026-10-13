# Design

## Context

El repo tiene solo el esqueleto generado por Spring Initializr: Spring Boot
4.1.1, Java 21, `spring-boot-starter-webmvc` y `spring-boot-starter-validation`,
lombok, un `application.properties` con el nombre de la app y un test
`contextLoads`. No hay specs previas. La motivación está en proposal.md (Why);
el comportamiento, en las specs `evaluacion-leads` y `acceso-api`.

Restricciones verificadas (docs/notas-tecnicas-leads.md): Jackson 3.1.5
(`tools.jackson`), Tomcat 11.0.24, y un spike de Drools 10.2.0 que funcionó
en jar ejecutable con records como hechos, un global y una sesión sin estado
por evaluación (~1 s de compilación al arrancar, ~20 µs por evaluación).

## Goals / Non-Goals

**Goals:**
- Que las condiciones de cada regla de negocio (tramos y umbrales) vivan en el
  DRL, y que los números (puntajes y umbrales) vivan en `application.yml`.
- Que todo error, incluidos los de los filtros, salga de un único
  `@RestControllerAdvice`.
- Que la lectura de la solicitud conserve el texto original de cada número,
  para que `leads-api-endurecer-formato-numerico` agregue regex sin rehacer el
  DTO.

**Non-Goals:**
- Recargar reglas o configuración en caliente: cualquier cambio requiere
  reiniciar.
- Compilar las reglas en tiempo de build (modelo ejecutable de Drools con
  `kie-maven-plugin`): se compilan al arrancar.
- Regla de edad, límite de cuerpo y regex estrictas (changes 2, 4 y 3).

## Endpoints

| Método y ruta | Request | Response 2xx | Errores posibles (`type` sin el prefijo `urn:leads-api:error:`) |
| --- | --- | --- | --- |
| POST /evaluar | Header `X-API-Key`; `Content-Type: application/json`; cuerpo JSON con los 12 campos (spec `evaluacion-leads`) | 200 `application/json;charset=UTF-8`: `{prioridad, score, motivos[], version_reglas}` | `no-autorizado` 401, `tipo-de-contenido-no-soportado` 415, `json-invalido` 400, `validacion` 400, `interno` 500 |
| Cualquier otra ruta | Header `X-API-Key` | — | `no-autorizado` 401, `no-encontrado` 404 |
| Otro método en /evaluar | Header `X-API-Key` | — | `no-autorizado` 401, `metodo-no-permitido` 405 |

Todas las respuestas de error son `application/problem+json;charset=UTF-8`
con `type`, `title`, `status` y, solo en `validacion`, `errores[]` con
`{campo, codigo, mensaje}`. `cuerpo-demasiado-grande` (413) queda en el
catálogo, sin código que lo produzca hasta el change 4.

## Decisions

### D1. Drools: reglas compiladas al arrancar desde el classpath

Un bean `KieBase` singleton se construye al arrancar: lee
`leads.reglas.archivo` (por defecto `classpath:reglas/evaluacion.drl`), lo
escribe en un `KieFileSystem` y compila con `KieBuilder.buildAll()`. Si hay
mensajes de nivel ERROR, lanza una excepción y la app no arranca. Cada
evaluación crea una `StatelessKieSession` desde ese `KieBase`, le pasa los
globals y ejecuta con los hechos `Solicitud` y `Resultado`.

- Dependencias: `org.drools:drools-bom:10.2.0` importado en
  `dependencyManagement`, más `drools-engine` y `drools-mvel`.
- Sin `kmodule.xml` ni escaneo de classpath: es lo que funcionó en el spike
  dentro del jar ejecutable.
- La ruta configurable existe para los escenarios de `version_reglas` que usan
  un DRL alternativo en tests.
- *Alternativas:* modelo ejecutable compilado en build (más rápido al
  arrancar, pero suma un plugin y generación de código; 1 s de arranque no lo
  justifica); `KieContainer` por classpath con `kmodule.xml` (frágil en jar
  ejecutable).

### D2. Una regla DRL por tramo, agrupadas por regla de negocio

Cada regla de negocio es un grupo de reglas DRL mutuamente excluyentes que
comparten el atributo `@regla("<id>")`. Exactamente una de cada grupo dispara
por solicitud. Excluyentes y de puntaje se separan por el tipo de motivo:

| `@regla` (regla de negocio) | Tipo de motivo | Reglas DRL (tramos) |
| --- | --- | --- |
| `urgencia` | PUNTAJE | `urgencia-esta-semana`, `urgencia-este-mes`, `urgencia-1-a-3-meses`, `urgencia-sin-apuro` |
| `documentacion` | PUNTAJE | `documentacion-si`, `documentacion-no` |
| `destino` | PUNTAJE | `destino-definido`, `destino-no-definido` |
| `situacion-laboral` | PUNTAJE | `situacion-dependiente`, `situacion-independiente`, `situacion-jubilado`, `situacion-estudiante`, `situacion-desempleado` |
| `antiguedad` | PUNTAJE | `antiguedad-alta`, `antiguedad-media`, `antiguedad-baja`, `antiguedad-no-aplica` |
| `clearing` | PUNTAJE | `clearing-si`, `clearing-no-se`, `clearing-no` |
| `carga` | PUNTAJE | `carga-baja`, `carga-media`, `carga-alta` |

En este change no hay reglas de tipo ELEGIBILIDAD; la de edad llega con
`leads-api-agregar-elegibilidad-edad` y va antes de las de puntaje.

Cada regla agrega al `Resultado` un motivo con `(regla, codigo, puntos)`. Los
puntos salen del global `puntajes`; los límites de los tramos (6 y 12 meses,
0,3 y 0,5), del global `umbrales`. El DRL no contiene números de negocio, ni
siquiera en los nombres de regla o en los códigos: los tramos se nombran
`baja`/`media`/`alta` porque sus límites son configurables. (Cambio hecho
durante el apply, con acuerdo de Julian: la versión aprobada nombraba los
tramos con sus valores, como `carga-hasta-30`, y quedaba desalineada si se
cambiaba un umbral.)

- *Alternativa descartada (explore):* una regla DRL por regla de negocio con
  el tramo calculado en Java; dejaba a Drools como un pasamanos.

### D3. El DRL emite códigos; Java arma los textos

El DRL es solo ASCII y nunca contiene textos en español. Una clase de textos
traduce `(regla, codigo, puntos)` a la frase de la spec y formatea el puntaje
(`+N`, `-N`, `0`) con guion ASCII. Los textos del recorte también se arman
ahí. Un test verifica que el DRL no tiene bytes fuera de ASCII.

Los textos de antigüedad y carga se arman con los umbrales configurados, no
con números fijos, para que un motivo nunca describa un corte distinto del
real: "hasta el {baja × 100}% del ingreso", "de más del {baja × 100}% y hasta
el {alta × 100}% del ingreso", "de más del {alta × 100}% del ingreso"; "de
{alto} meses o más", "de {medio} a {alto − 1} meses", "de menos de {medio}
meses". Con la configuración por defecto (0,3; 0,5; 6; 12) dan exactamente
los textos de la spec.

- *Motivo:* el spike mostró que Drools corrompe caracteres no ASCII si la JVM
  arranca con otra codificación por defecto (notas técnicas, "Spike de
  Drools").

### D4. Hechos y globals

- `Solicitud`: record con los valores ya validados. Códigos como enums con
  mapeo explícito a los valores del contrato (`1_a_3_meses` →
  `DE_1_A_3_MESES`). Importes como `BigDecimal`, enteros como `Integer`.
- La carga se compara sin dividir con un método del hecho que el DRL invoca
  con el umbral: `Solicitud( cargaNoSupera(umbrales.cargaBaja()) )`, donde
  `cargaNoSupera(f)` es `cuota * plazo + monto <= f * ingreso * plazo` con
  `compareTo`. La aritmética BigDecimal queda en Java y la elección del tramo
  en el DRL.
- `Resultado`: acumula los motivos emitidos.
- Globals: `puntajes` y `umbrales`, records de configuración (D7).

### D5. Score, orden y prioridad en Java

Después de ejecutar la sesión, Java:
1. Verifica que haya exactamente un motivo por cada `@regla` esperada; si no,
   es un error interno (500), nunca una respuesta con motivos de menos.
2. Ordena los motivos por la posición fija de su regla de negocio (urgencia,
   documentación, destino, situación laboral, antigüedad, clearing, carga).
3. Suma, recorta a [0, 100] y agrega el motivo de recorte si corresponde.
4. Asigna la prioridad con los umbrales de `leads.reglas.prioridad`.

### D6. Lectura de la solicitud con el texto original de cada valor

El controller recibe un tipo propio, `SolicitudCruda`, con un deserializador
Jackson que recorre el objeto y guarda por campo el tipo de token y el texto
original (`JsonParser.getString()` para números y strings), sin convertir a
double. Si la raíz no es un objeto, lo marca para el error `tipo-invalido` en
`$`. Los campos desconocidos se descartan sin registrarse.

Un validador recorre los 12 campos y acumula errores en orden de campo:
ausente o null → `requerido`; tipo de token incorrecto → `tipo-invalido`;
texto no convertible, entero con decimales o importe con más de 2 decimales o
12 dígitos enteros → `formato-invalido`; fuera de los valores de código →
`valor-no-permitido`; fuera de rango → `fuera-de-rango`. Con errores, lanza
una excepción de validación con la lista; sin errores, construye `Solicitud`.

- Un JSON que no se puede parsear (incluido un número que supera
  `maxNumberLength`, 1000 por defecto en este change) llega como
  `HttpMessageNotReadableException` → `json-invalido`.
- *Alternativas:* DTO con `BigDecimal`/`Integer` y Bean Validation (Jackson
  corta en el primer error y pierde el texto original); `JsonNode` (pierde el
  texto original de los números, por ejemplo `1e3` frente a `1000`).
- Esto reemplaza "tipos envoltorio con @NotNull" de la decisión 13: la
  obligatoriedad se valida sobre el valor crudo.

### D7. Configuración

`application.yml` reemplaza a `application.properties`:

```yaml
spring:
  application:
    name: backend-leads
leads:
  seguridad:
    api-key: ${LEADS_API_KEY:}
  reglas:
    archivo: classpath:reglas/evaluacion.drl
    prioridad: { alta: 60, media: 30 }
    umbrales:
      antiguedad: { meses-alto: 12, meses-medio: 6 }
      carga: { baja: 0.3, alta: 0.5 }
    puntajes:
      urgencia: { esta-semana: 20, este-mes: 15, de-1-a-3-meses: 5, sin-apuro: 0 }
      documentacion: { si: 15, no: 0 }
      destino: { definido: 5, no-definido: 0 }
      situacion-laboral: { dependiente: 15, independiente: 10, jubilado: 15, estudiante: 0, desempleado: -10 }
      antiguedad: { doce-o-mas: 10, seis-a-once: 5, menos-de-seis: 0, no-aplica: 0 }
      clearing: { si: -30, no-se: -5, no: 0 }
      carga: { baja: 20, media: 5, alta: -15 }
```

- Records con `@ConfigurationProperties` y `@Validated`. Puntajes y umbrales
  como `BigDecimal`, para que `20.0` se acepte igual que `20`; los puntajes
  deben ser enteros y los umbrales de antigüedad, enteros positivos.
- Coherencia validada al arrancar: `prioridad.alta > prioridad.media >= 0`,
  `0 < carga.baja < carga.alta`, `0 < antiguedad.meses-medio <
  antiguedad.meses-alto`, ningún valor faltante.
- `${LEADS_API_KEY:}` usa vacío como defecto para que la falta de clave dé el
  mensaje propio de D9 y no un error genérico de placeholder.

### D8. version_reglas

SHA-256 sobre el texto UTF-8 de: la configuración de `leads.reglas`
(`prioridad`, `umbrales`, `puntajes`) aplanada a líneas `clave=valor`
ordenadas por clave, con cada `BigDecimal` normalizado con
`stripTrailingZeros().toPlainString()`; un separador; el contenido del DRL con
`\r\n` reemplazado por `\n`; un separador; la constante `REVISION_LOGICA`. Se
toman los primeros 12 caracteres hexadecimales en minúscula. Se calcula una
vez al arrancar.

- `REVISION_LOGICA` es una constante entera en Java que se sube a mano cuando
  cambia la lógica en Java (D3, D5) o un texto.
- Test de cierre: guarda la lista esperada de reglas DRL con su `@regla`
  (leída del `KieBase`) junto con la revisión esperada. Si la lista del
  `KieBase` cambia y `REVISION_LOGICA` no, falla con un mensaje que indica
  qué actualizar.
- `leads.reglas.archivo` no entra en el hash: la ruta no cambia la lógica,
  su contenido sí.

### D9. Clave de API con un filtro propio

Un `OncePerRequestFilter` con la mayor precedencia compara el header con la
clave: calcula el SHA-256 de ambas y las compara con `MessageDigest.isEqual`
(tiempo constante, sin filtrar el largo). Si no coincide, delega una
excepción en el `HandlerExceptionResolver` para que el advice responda 401.
Nunca lee el cuerpo.

La clave se valida al arrancar en un record `@ConfigurationProperties`
(`leads.seguridad`): presente, al menos 32 caracteres, solo ASCII imprimible
(0x21 a 0x7E, sin espacio). El mensaje de error nombra la condición y nunca el
valor.

- *Alternativa descartada (explore):* Spring Security; no trae autenticación
  por clave y obliga a desactivar CSRF, sesiones y login.

### D10. Errores

Un `@RestControllerAdvice` que extiende `ResponseEntityExceptionHandler`:

| Excepción | `type` | Estado |
| --- | --- | --- |
| Clave inválida (desde el filtro) | `no-autorizado` | 401 |
| Validación (D6) | `validacion` | 400 |
| `HttpMessageNotReadableException` | `json-invalido` | 400 |
| `NoResourceFoundException` | `no-encontrado` | 404 |
| `HttpRequestMethodNotSupportedException` | `metodo-no-permitido` | 405 |
| `HttpMediaTypeNotSupportedException` | `tipo-de-contenido-no-soportado` | 415 |
| Cualquier otra | `interno` | 500 |

- El advice sobrescribe la construcción del ProblemDetail para que ningún
  caso salga con `about:blank`; un test recorre los casos.
- El Content-Type se fija explícito en cada respuesta:
  `application/problem+json;charset=UTF-8` en errores y
  `application/json;charset=UTF-8` en la respuesta 200. No se usa `produces`
  en el mapping.
- La negociación de contenido ignora el header Accept
  (`ContentNegotiationConfigurer.ignoreAcceptHeader(true)` con JSON por
  defecto), así nunca se produce 406. Decisión confirmada en el propose:
  solo hay un formato de respuesta y rechazar una evaluación válida por el
  Accept no aporta nada.
- `interno` no incluye `detail` ni datos de la excepción.

### D11. Logging

- El validador guarda el `session_id` en un atributo del request solo si
  cumple su formato.
- Evaluación exitosa: una línea INFO `evaluacion session_id=<id>
  prioridad=<p> score=<s> version_reglas=<v>`.
- Error: una línea WARN `error type=<urn> status=<n>` más
  ` session_id=<id>` si el atributo existe. El advice escribe esta línea; el
  500 además registra la excepción a nivel ERROR sin datos del cuerpo.
- Nada más se registra: ni cuerpo, ni valores, ni mensajes de error de campo,
  ni headers.

### D12. Paquetes

```
com.sagant.leads.leads_api
  evaluacion/api/       controller, SolicitudCruda y su deserializador, validador
                        (traduce el contrato a Solicitud), respuesta
  evaluacion/dominio/   Solicitud, enums de códigos, Resultado, Motivo, cálculo
                        de score y prioridad
  evaluacion/reglas/    KieBase, ejecución de la sesión, textos de motivos,
                        version_reglas
  evaluacion/config/    records de leads.reglas y su validación de coherencia
  seguridad/            filtro de clave, record de leads.seguridad
  comun/error/          advice, catálogo de type, excepciones
src/main/resources/reglas/evaluacion.drl
```

### D13. Build

Se sacan lombok y su `annotationProcessorPaths` en las dos ejecuciones del
compilador; se agrega el BOM de Drools y sus dos dependencias. El test
`contextLoads` existente recibe una clave ficticia por propiedad de test.

## Risks / Trade-offs

- [Un método con argumento global en una restricción del DRL no se probó en el
  spike] → El grupo de tareas del motor empieza con la regla de carga y sus
  bordes; si falla, la comparación pasa a una restricción con variables
  enlazadas, sin cambiar la spec.
- [Dos tramos de una misma regla se superponen o falta uno] → Test "exactamente
  un motivo por regla de negocio" sobre todas las combinaciones de la tabla de
  máximos, más los escenarios de borde.
- [Drools suma ~30 dependencias y ~1 s de arranque] → Aceptado en el explore.
- [Cambios solo de formato en el DRL cambian `version_reglas`] → Aceptado: es
  más seguro sobrar versiones que perder un cambio real.
- [Olvidar subir `REVISION_LOGICA` al cambiar Java] → El test de cierre cubre
  cambios de reglas; los cambios de textos y de cálculo dependen de la
  revisión del PR. Queda en la lista de revisión del código.
- [La sección "Convenciones" de las specs podría no sobrevivir al archive] →
  La solicitud base está dentro de un escenario. Al archivar se comprueba la
  spec principal (paso 14 del flujo).

## Migration Plan

1. n8n se actualiza para mandar `edad` y `antiguedad_meses`, dejar de mandar
   `nombre` y leer `version_reglas`, antes de la primera conexión.
2. Para correr en desarrollo: definir `LEADS_API_KEY` (32 caracteres o más,
   por ejemplo con `openssl rand -base64 32`) y la misma clave en el nodo HTTP
   de n8n.
3. Sin producción previa: no hay rollback más allá de no exponer el servicio.
