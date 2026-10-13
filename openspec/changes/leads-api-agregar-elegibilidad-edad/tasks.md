# Tasks

Convención: los escenarios se nombran entre comillas angulares, exactamente
como en la spec delta. Los tests de escenario llevan `@DisplayName` con ese
nombre. Cada grupo termina con `./mvnw test` en verde.

Excepción: los grupos 2, 3 y 4 se implementan como un solo bloque. Las reglas
de edad (2.2) hacen fallar el cálculo, los textos, el test de cierre y los
tests HTTP hasta que estén los grupos 3 y 4, y separarlos pediría código
provisorio. Las tareas de esos grupos se marcan recién con la suite completa en
verde al final del bloque.

## 1. Configuración de la edad mínima

- [x] 1.1 Agregar `edadMinima` a `ReglasProperties.Umbrales` (D1) y `leads.reglas.umbrales.edad-minima: 18` a `application.yml`, con un comentario sobre el plural con edad mínima 1; verificar con el test de configuración por defecto que se carga 18
- [x] 1.2 Tests de arranque de «Edad mínima faltante», «Edad mínima 0», «Edad mínima 101» y «Edad mínima con decimales», más los bordes 1 y 100 que arrancan; verificar que el contexto falla y el mensaje nombra la propiedad

## 2. Regla de edad y motivos de elegibilidad

- [x] 2.1 Crear `TipoMotivo`, sumar el tipo a `Motivo` con las constantes `elegible` y `no-elegible`, y `agregarElegibilidad` a `Resultado` (D2); verificar que compila y que los tests del change 1 siguen pasando con los motivos de PUNTAJE
- [x] 2.2 Agregar `Solicitud.edadAlMenos` y las reglas `edad-elegible` y `edad-no-elegible` con `emitirElegibilidad` (D3); verificar con una tabla de casos los bordes 1, 17, 18 y 100 con edad mínima 18, y 20 y 21 con edad mínima 21
- [x] 2.3 Actualizar el test "exactamente un motivo por regla de negocio" a 8 reglas (incluida `edad`) y sumar edades 1, 17, 18 y 100 a las combinaciones; verificar que las 25 reglas DRL disparan alguna vez y que el DRL sigue siendo ASCII y sin números

## 3. Precedencia, textos y versión

- [x] 3.1 Sumar `REGLAS_EXCLUYENTES`, el orden excluyentes-primero, la suma solo de PUNTAJE y la precedencia del ajuste en `CalculoEvaluacion` (D4); verificar con una tabla de casos: no elegible con sumas -55, 0, 85 y 105 (siempre score 0, BAJA y un solo motivo final de ajuste), no elegible con umbral de MEDIA en 0 (BAJA) y elegible con -55 y 105 (recorte, sin ajuste)
- [x] 3.2 Sumar los textos de elegibilidad y `ajustePorInelegibilidad` (D5); verificar con una tabla que "Edad de 18 años o más: elegible", "No elegible: menor de 18 años" y "Score ajustado a 0 por inelegibilidad (suma 85)" son exactos, que con edad mínima 21 los textos dicen 21 y que los de elegibilidad no llevan puntaje
- [x] 3.3 Test que verifica el orden completo de los motivos: 8 sin motivo final, 9 con recorte o con ajuste, el de edad siempre primero y el motivo final una sola vez y al final
- [x] 3.4 Subir `REVISION_LOGICA` a 2 y actualizar el test de cierre con `edad-elegible@edad` y `edad-no-elegible@edad` (D6); verificar que pasa y que `version_reglas` cambia respecto del change 1

## 4. Rango de edad y tests HTTP

- [x] 4.1 Cambiar el rango de `edad` a 1 a 100 en `ValidadorSolicitud` (D6) y actualizar su tabla de casos (0 y 101 fuera de rango; 1, 13, 14 y 100 válidos)
- [x] 4.2 Tests HTTP de los requirements modificados con cambios: «Solicitud base evaluada», «Edad 0», «Edad 1», «Edad 13», «Edad 14», «Suma negativa», «Orden completo de la solicitud base», «Motivo de recorte al final y una sola vez», «Motivo de ajuste al final y una sola vez», «Caracteres no ASCII en UTF-8» y «Motivo de elegibilidad sin puntaje»
- [x] 4.3 Volver a correr los tests HTTP de los escenarios sin cambios de los requirements modificados y ajustar los que cuentan motivos: «Campo extra ignorado», «Cuerpo que no es un objeto», «Monto cero», «Monto mínimo positivo», «Ingreso negativo», «Ingreso cero», «Cuota de deudas negativa», «Cuota de deudas cero», «Plazo 0», «Plazo 1», «Plazo 120», «Plazo 121», «Edad 100», «Edad 101», «Antigüedad -1», «Antigüedad 0», «Antigüedad 600», «Antigüedad 601», «Suma exactamente 0», «Suma mayor que 100», «Suma exactamente 100», «Solicitud inválida sin score», «Error sin motivos», «Puntaje negativo con guion ASCII» y «Error sin textos de motivos»
- [x] 4.4 Tests HTTP de «Edad 17 no elegible», «Edad 18 elegible», «Edad 1 no elegible», «Edad 100 elegible», «Menor con perfil de prioridad alta» y «Edad cero no se evalúa»
- [x] 4.5 Tests HTTP de «Menor con suma entre 0 y 100», «Menor con suma 0», «Menor con suma negativa», «Menor con suma mayor que 100» (configuración de prueba), «Mayor de edad con suma negativa», «Menor con prioridad media configurada en 0» (configuración de prueba) y «Solicitud inválida sin ajuste»
- [x] 4.6 Tests HTTP de «Edad mínima 21 con edad 20» y «Edad mínima 21 con edad 21», con la edad mínima en 21
- [x] 4.7 Verificar con el script de cobertura que cada escenario de las specs principales y del delta tiene un test con su `@DisplayName`, sin nombres huérfanos

## 5. Documentación

- [x] 5.1 Actualizar `docs/roadmap.md` (change 2 implementado) y `docs/notas-tecnicas-leads.md` (tabla de máximos sin cambios para mayores; texto de los motivos de edad); verificar que no contradicen la spec

## 6. Cierre

- [x] 6.1 Correr `./mvnw test` completo; verificar que todos los tests pasan
- [x] 6.2 Correr `openspec validate leads-api-agregar-elegibilidad-edad --strict`; verificar que pasa sin advertencias
- [ ] 6.3 Revisión del código por una persona en el PR (paso 13 del flujo); verificar que no quedan comentarios sin resolver

## Workflow follow-up

- `/opsx:archive` como último commit del branch, con el PR revisado y los tests en verde.
- Después del archive, comprobar `openspec/specs/evaluacion-leads/spec.md`.
