# Notas técnicas de leads-api

Resolución de los puntos "A VERIFICAR Y AFINAR" del explore inicial
(2026-10-09). Cada punto tiene la resolución, la evidencia y cómo se testea.
El plan de changes está en [roadmap.md](roadmap.md).

## Versiones resueltas

Evidencia: `./mvnw dependency:tree` sobre el `pom.xml` actual, y clases
inspeccionadas con `javap` en los jars de `~/.m2`.

| Pieza | Versión |
| --- | --- |
| Spring Boot (parent) | 4.1.1 |
| Java | 21 |
| Jackson (databind y core, groupId `tools.jackson.core`) | 3.1.5 |
| jackson-annotations (`com.fasterxml.jackson.core`) | 2.21 |
| Tomcat embebido | 11.0.24 |

`spring-boot-starter-webmvc` trae Jackson 3 a través de
`spring-boot-starter-jackson` y `spring-boot-jackson`.

## a. Longitud máxima de números en Jackson 3

**Resolución:** se configura por propiedad, sin código:

```yaml
spring:
  jackson:
    factory:
      constraints:
        read:
          max-number-length: 20
```

**Evidencia:**
- `tools.jackson.core.StreamReadConstraints` (jackson-core 3.1.5) tiene
  `DEFAULT_MAX_NUM_LEN = 1000`, `getMaxNumberLength()` y
  `Builder.maxNumberLength(int)`.
- `TSFBuilder.streamReadConstraints(StreamReadConstraints)` lo aplica a la
  factory.
- spring-boot-jackson 4.1.1 expone la propiedad
  `spring.jackson.factory.constraints.read.max-number-length` en su
  `spring-configuration-metadata.json`. También existen `max-nesting-depth`,
  `max-string-length`, `max-document-length`, `max-name-length` y
  `max-token-count`.
- La aplica `JacksonAutoConfiguration`, a través de su
  `StandardJsonFactoryBuilderCustomizer`.
- Si hiciera falta código, el punto de extensión es
  `org.springframework.boot.jackson.autoconfigure.JsonFactoryBuilderCustomizer`.

**Valor:** 20. El número válido más largo tiene 15 caracteres (12 dígitos
enteros, punto y 2 decimales). El margen permite que un número algo más largo
llegue a la validación y reciba un `formato_invalido` claro, en lugar de un
error del parser.

**Comportamiento:** un número más largo lanza
`tools.jackson.core.exc.StreamConstraintsException` durante el parseo. Spring
la envuelve en `HttpMessageNotReadableException`, y el advice la mapea a 400
(`json_invalido`), nunca a 500.

**Test:**
- Un número JSON de 1000 dígitos da 400 con `type` `json_invalido`.
- Un número de 21 caracteres da 400.
- Un número de 20 caracteres llega a la validación y da `formato_invalido`.

**Nota:** un string numérico ("123...") no pasa por esta restricción. Lo
cubren la regex y `max-string-length`.

## b. Límite del cuerpo de 16 KB

**Pieza:** un `OncePerRequestFilter` propio (`comun/web`). Envuelve el request
en un `HttpServletRequestWrapper` cuyo `getInputStream()` devuelve un
`ServletInputStream` que cuenta los bytes leídos. Al superar 16384 bytes lanza
`CuerpoDemasiadoGrandeException`.

- **Con Content-Length:** si declara más de 16384, se rechaza antes de leer.
- **Chunked (sin Content-Length):** el contador corta la lectura en el byte
  16385, sin importar lo que diga el header. Por eso cubre los dos casos.
- La excepción se lanza mientras Jackson lee, así que llega envuelta en
  `HttpMessageNotReadableException`. El advice debe revisar la cadena de causas
  y responder 413 (`cuerpo_demasiado_grande`), no 400.
- El filtro delega en `HandlerExceptionResolver` para que el error pase por el
  mismo advice.
- Las propiedades de Tomcat no sirven para esto: `max-http-form-post-size`
  aplica solo a formularios y `max-swallow-size` al cuerpo descartado.

**Tests:**
- MockMvc con un Content-Length de 16384 (pasa) y de 16385 (413).
- Chunked: `@SpringBootTest(webEnvironment = RANDOM_PORT)` con
  `java.net.http.HttpClient` y `BodyPublishers.ofInputStream(...)`. Con un
  largo desconocido, el cliente envía `Transfer-Encoding: chunked`. 20 KB dan
  413, y el test verifica que no hay header Content-Length.
- El filtro de tamaño también se puede testear de forma unitaria con un
  request simulado que devuelva `getContentLength() == -1`.

## c. Número fuera de formato vs. tipo incorrecto

**Problema:** si los campos numéricos del DTO son `BigDecimal` o `Integer`,
Jackson corta en el primer error con un mensaje propio. Así no se pueden
listar todos los errores juntos (decisión 11) ni distinguir los casos.

**Resolución:** el DTO de entrada guarda cada campo numérico en forma cruda. La
opción recomendada es un tipo propio que registra el tipo de token JSON y el
texto original (`JsonParser.getString()` sobre un token numérico, sin pasar
por double; en Jackson 3 `getText()` sigue existiendo pero `getString()` es
el método principal). `JsonNode` es la alternativa. El validador:
1. Token número o string: aplica la regex del campo al texto. Si no cumple,
   `formato_invalido` ("200.5555: formato numérico inválido, máximo 2
   decimales").
2. Token booleano, array u objeto: `tipo_invalido` ("se esperaba un número").
3. Null o ausente: `requerido`.
4. Formato válido: convierte a BigDecimal o Integer y valida el rango
   (`fuera_de_rango`).

Se acumulan todos los errores y se responde un solo 400.

**Test:** una petición con `monto: 200.5555` y `plazo_meses: true` da un 400
con dos errores, `formato_invalido` en monto y `tipo_invalido` en plazo_meses.
Más un escenario por cada entrada de la decisión 13.

## d. Orden 401 > 413

**Resolución:** el filtro de clave se registra con un orden menor que el de
tamaño y nunca lee el cuerpo. Responde 401 antes de que se consuma un solo
byte.

**Tests:**
- 20 KB sin clave: 401.
- 20 KB con clave válida: 413.
- 20 KB con clave válida y JSON inválido: 413, no 400.
- Ruta desconocida sin clave: 401; con clave válida: 404.
- GET /evaluar sin clave: 401; con clave válida: 405.

## e. Charset UTF-8

**Resolución:** Jackson escribe JSON en UTF-8. Spring no agrega
`charset` a `application/json` (RFC 8259 define UTF-8 como codificación del
JSON). Falta decidir si se declara explícito (roadmap, change 1).

**Trampa:** en MockMvc, `getContentAsString()` decodifica en ISO-8859-1 cuando
la respuesta no declara charset. Un test con esa llamada puede fallar aunque el
servicio esté bien, o pasar por error en la comparación inversa.

**Test:** decodificar `getContentAsByteArray()` con
`StandardCharsets.UTF_8` y verificar:
- "más" en "Carga de deuda de más del 50% del ingreso: -15";
- "años" en "No elegible: menor de 18 años" (desde el change 2);
- que ningún motivo contiene U+2212 (signo menos Unicode), solo el guion ASCII.

## f. Test de máximos por situación laboral

**Tabla verificada a mano** con los puntajes de la especificación:

| Situación laboral | Urg. | Doc. | Destino | Lab. | Antig. | Carga | Máximo | Con clearing = si |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| dependiente | 20 | 15 | 5 | 15 | 10 | 20 | 85 | 55 |
| independiente | 20 | 15 | 5 | 10 | 10 | 20 | 80 | 50 |
| jubilado | 20 | 15 | 5 | 15 | 0 | 20 | 75 | 45 |
| estudiante | 20 | 15 | 5 | 0 | 0 | 20 | 60 | 30 |
| desempleado | 20 | 15 | 5 | -10 | 0 | 20 | 50 | 20 |

El mínimo absoluto es -55 (desempleado, clearing si y carga > 0,5), que
coincide con el ejemplo de la decisión 2. El máximo absoluto es 85, así que el
recorte a 100 no se alcanza con la configuración real. Por eso se prueba con una
configuración de prueba (decisión 2).

**Método recomendado:** enumerar el dominio en vez de pedirle a cada regla su
máximo. Para cada situación laboral y cada valor de clearing, se recorren:
- todas las urgencias, documentación y destinos;
- un valor representativo por tramo de antigüedad (0, 6 y 12 meses);
- un valor representativo por tramo de carga.

Se evalúan las reglas de la configuración efectiva (el bean real) y se toma el
máximo. El test compara contra la tabla de la spec. Si alguien cambia un
puntaje en `application.yml`, falla.

## Otras notas

- **Carga sin dividir:** `carga <= 0,3` equivale a
  `cuota_deudas * plazo + monto <= 0,3 * ingreso * plazo` (plazo > 0). Con
  `ingreso_mensual = 0`, el lado derecho es 0 y el izquierdo es mayor que 0
  (monto > 0), así que cae en "> 0,5" sin caso especial. Bordes exactos con
  `compareTo`, nunca con `equals` (este último compara también la escala).
- **Forma canónica de `version_reglas`:** normalizar cada BigDecimal con
  `stripTrailingZeros().toPlainString()`. Sin `toPlainString`, 20 se vuelve
  "2E+1" y 20.0 da "20". Con las dos llamadas, 20 y 20.0 dan "20".
- **Clave en tiempo constante:** `MessageDigest.isEqual` sobre el SHA-256 de
  la clave recibida y de la esperada. Comparar los hashes, que tienen el mismo
  largo, evita filtrar el largo de la clave.
- **Campos extra:** dejar `FAIL_ON_UNKNOWN_PROPERTIES` en false de forma
  explícita y cubrirlo con un test (campo `nombre` presente: 200).
