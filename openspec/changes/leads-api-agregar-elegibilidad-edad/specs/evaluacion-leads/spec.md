# Spec Delta

## MODIFIED Requirements

### Requirement: Evaluación de una solicitud válida
El sistema SHALL aceptar en POST /evaluar un objeto JSON con los 12 campos de
la solicitud base y responder 200 con `prioridad`, `score`, `motivos` y
`version_reglas`. Los campos que no forman parte del contrato se ignoran.

#### Scenario: Solicitud base evaluada
- **WHEN** se envía POST /evaluar con la solicitud base, que es este JSON: `{"session_id": "beb55e7eb0c947ff86f19aa0e0d0b2db", "edad": 32, "motivo": "auto", "monto": 150000, "plazo_meses": 24, "urgencia": "esta_semana", "situacion_laboral": "dependiente", "antiguedad_meses": 36, "ingreso_mensual": 60000, "cuota_deudas": 5000, "clearing": "no", "documentacion": "si"}`
- **THEN** responde 200 con `prioridad` "ALTA", `score` 85, 8 motivos y un `version_reglas`

#### Scenario: Campo extra ignorado
- **WHEN** se envía POST /evaluar con la solicitud base más `"nombre": "Persona Ficticia"`
- **THEN** responde 200 con el mismo cuerpo que la solicitud base

#### Scenario: Cuerpo que no es un objeto
- **WHEN** se envía POST /evaluar con el cuerpo `[1, 2]`
- **THEN** responde 400 `validacion` con `tipo-invalido` en el campo `$`

### Requirement: Campos requeridos
Los 12 campos de la solicitud son requeridos. Un campo ausente o con valor
null MUST producir un error `requerido`. El sistema SHALL devolver todos los
errores de la solicitud juntos en un único 400.

#### Scenario: Campo ausente
- **WHEN** se envía POST /evaluar con la solicitud base sin `edad`
- **THEN** responde 400 `validacion` con `requerido` en `edad`

#### Scenario: Campo null
- **WHEN** se envía POST /evaluar con la solicitud base y `antiguedad_meses` null
- **THEN** responde 400 `validacion` con `requerido` en `antiguedad_meses`

#### Scenario: Objeto vacío
- **WHEN** se envía POST /evaluar con el cuerpo `{}`
- **THEN** responde 400 `validacion` con 12 errores `requerido`, uno por campo

#### Scenario: Varios errores en una misma solicitud
- **WHEN** se envía POST /evaluar con la solicitud base, `edad` 0 y `clearing` "talvez"
- **THEN** responde 400 `validacion` con exactamente 2 errores: `fuera-de-rango` en `edad` y `valor-no-permitido` en `clearing`

### Requirement: Rangos de los números
Los números SHALL cumplir estos rangos, con los extremos incluidos: `monto`
mayor que 0; `ingreso_mensual` y `cuota_deudas` mayores o iguales a 0;
`plazo_meses` de 1 a 120; `edad` de 1 a 100; `antiguedad_meses` de 0 a 600.
Fuera de rango MUST dar `fuera-de-rango`.

#### Scenario: Monto cero
- **WHEN** se envía POST /evaluar con la solicitud base y `monto` 0
- **THEN** responde 400 `validacion` con `fuera-de-rango` en `monto`

#### Scenario: Monto mínimo positivo
- **WHEN** se envía POST /evaluar con la solicitud base y `monto` 0.01
- **THEN** responde 200

#### Scenario: Ingreso negativo
- **WHEN** se envía POST /evaluar con la solicitud base y `ingreso_mensual` -0.01
- **THEN** responde 400 `validacion` con `fuera-de-rango` en `ingreso_mensual`

#### Scenario: Ingreso cero
- **WHEN** se envía POST /evaluar con la solicitud base y `ingreso_mensual` 0
- **THEN** responde 200

#### Scenario: Cuota de deudas negativa
- **WHEN** se envía POST /evaluar con la solicitud base y `cuota_deudas` -0.01
- **THEN** responde 400 `validacion` con `fuera-de-rango` en `cuota_deudas`

#### Scenario: Cuota de deudas cero
- **WHEN** se envía POST /evaluar con la solicitud base y `cuota_deudas` 0
- **THEN** responde 200

#### Scenario: Plazo 0
- **WHEN** se envía POST /evaluar con la solicitud base y `plazo_meses` 0
- **THEN** responde 400 `validacion` con `fuera-de-rango` en `plazo_meses`

#### Scenario: Plazo 1
- **WHEN** se envía POST /evaluar con la solicitud base y `plazo_meses` 1
- **THEN** responde 200

#### Scenario: Plazo 120
- **WHEN** se envía POST /evaluar con la solicitud base y `plazo_meses` 120
- **THEN** responde 200

#### Scenario: Plazo 121
- **WHEN** se envía POST /evaluar con la solicitud base y `plazo_meses` 121
- **THEN** responde 400 `validacion` con `fuera-de-rango` en `plazo_meses`

#### Scenario: Edad 0
- **WHEN** se envía POST /evaluar con la solicitud base y `edad` 0
- **THEN** responde 400 `validacion` con `fuera-de-rango` en `edad`

#### Scenario: Edad 1
- **WHEN** se envía POST /evaluar con la solicitud base y `edad` 1
- **THEN** responde 200

#### Scenario: Edad 13
- **WHEN** se envía POST /evaluar con la solicitud base y `edad` 13
- **THEN** responde 200 con `prioridad` "BAJA" y `score` 0, no 400: un menor de 14 es no elegible, no un error

#### Scenario: Edad 14
- **WHEN** se envía POST /evaluar con la solicitud base y `edad` 14
- **THEN** responde 200 con `prioridad` "BAJA" y `score` 0

#### Scenario: Edad 100
- **WHEN** se envía POST /evaluar con la solicitud base y `edad` 100
- **THEN** responde 200

#### Scenario: Edad 101
- **WHEN** se envía POST /evaluar con la solicitud base y `edad` 101
- **THEN** responde 400 `validacion` con `fuera-de-rango` en `edad`

#### Scenario: Antigüedad -1
- **WHEN** se envía POST /evaluar con la solicitud base y `antiguedad_meses` -1
- **THEN** responde 400 `validacion` con `fuera-de-rango` en `antiguedad_meses`

#### Scenario: Antigüedad 0
- **WHEN** se envía POST /evaluar con la solicitud base y `antiguedad_meses` 0
- **THEN** responde 200

#### Scenario: Antigüedad 600
- **WHEN** se envía POST /evaluar con la solicitud base y `antiguedad_meses` 600
- **THEN** responde 200

#### Scenario: Antigüedad 601
- **WHEN** se envía POST /evaluar con la solicitud base y `antiguedad_meses` 601
- **THEN** responde 400 `validacion` con `fuera-de-rango` en `antiguedad_meses`

### Requirement: Score y recorte
Para una solicitud elegible, el score SHALL ser max(0, min(100, suma de los
puntos de los motivos de puntaje)). Si se aplica un recorte, se agrega al
final un motivo sin puntos: "Score limitado al mínimo de 0 (suma <suma>)" o
"Score limitado al máximo de 100 (suma <suma>)". Una suma de exactamente 0 o
100 MUST NOT agregar motivo de recorte. Para una solicitud no elegible rige
«Ajuste por inelegibilidad» y nunca hay recorte.

#### Scenario: Suma negativa
- **WHEN** se envía POST /evaluar con la solicitud base, `situacion_laboral` "desempleado", `antiguedad_meses` 0, `urgencia` "sin_apuro", `documentacion` "no", `motivo` "otro", `clearing` "si" y `cuota_deudas` 60000
- **THEN** responde 200 con `score` 0, `prioridad` "BAJA", el último motivo "Score limitado al mínimo de 0 (suma -55)" y ningún motivo de inelegibilidad

#### Scenario: Suma exactamente 0
- **WHEN** se envía POST /evaluar con la solicitud base, `situacion_laboral` "estudiante", `antiguedad_meses` 0, `urgencia` "sin_apuro", `documentacion` "no", `motivo` "otro", `clearing` "no_se" y `cuota_deudas` 20000
- **THEN** responde 200 con `score` 0 y ningún motivo de recorte

#### Scenario: Suma mayor que 100
- **WHEN** con una configuración de prueba en la que la solicitud base suma 105, se envía POST /evaluar con la solicitud base
- **THEN** responde 200 con `score` 100, `prioridad` "ALTA" y el último motivo "Score limitado al máximo de 100 (suma 105)"

#### Scenario: Suma exactamente 100
- **WHEN** con una configuración de prueba en la que la solicitud base suma 100, se envía POST /evaluar con la solicitud base
- **THEN** responde 200 con `score` 100 y ningún motivo de recorte

#### Scenario: Solicitud inválida sin score
- **WHEN** se envía POST /evaluar con la solicitud base sin `monto`
- **THEN** responde 400 `validacion` sin `score`

### Requirement: Orden y forma de los motivos
`motivos` SHALL listar primero el motivo de elegibilidad por edad; después,
exactamente un motivo por regla de puntaje, también cuando suma 0, en este
orden: urgencia, documentación, destino del préstamo, situación laboral,
antigüedad, clearing, carga; y al final, si corresponde, un único motivo de
recorte o de ajuste por inelegibilidad: 8 motivos, o 9. La suma de los puntos
de los motivos de puntaje MUST reproducir la suma antes del recorte o ajuste.

#### Scenario: Orden completo de la solicitud base
- **WHEN** se envía POST /evaluar con la solicitud base
- **THEN** responde 200 con `motivos` exactamente: "Edad de 18 años o más: elegible", "Urgencia esta semana: +20", "Documentación de ingresos disponible: +15", "Destino del préstamo definido: +5", "Empleado en relación de dependencia: +15", "Antigüedad laboral de 12 meses o más: +10", "No figura en el Clearing de Informes: 0", "Carga de deuda hasta el 30% del ingreso: +20"

#### Scenario: Motivo de recorte al final y una sola vez
- **WHEN** se envía POST /evaluar con la solicitud del escenario "Suma negativa"
- **THEN** responde 200 con 9 motivos, el primero es "Edad de 18 años o más: elegible", el motivo de recorte es el último y aparece una sola vez

#### Scenario: Motivo de ajuste al final y una sola vez
- **WHEN** se envía POST /evaluar con la solicitud base y `edad` 16
- **THEN** responde 200 con 9 motivos, el primero es "No elegible: menor de 18 años", el motivo de ajuste es el último, aparece una sola vez y no hay motivo de recorte

#### Scenario: Error sin motivos
- **WHEN** se envía POST /evaluar con la solicitud base sin `urgencia`
- **THEN** responde 400 `validacion` sin `motivos`

### Requirement: Formato de los textos de los motivos
El puntaje en cada motivo de puntaje SHALL escribirse `+N` si es positivo,
`-N` si es negativo y `0` sin signo, con el guion ASCII (U+002D) y nunca el
signo menos Unicode (U+2212). Los motivos de elegibilidad, de recorte y de
ajuste no llevan puntaje. Los textos se envían en UTF-8.

#### Scenario: Puntaje negativo con guion ASCII
- **WHEN** se envía POST /evaluar con la solicitud base y `clearing` "si"
- **THEN** responde 200 y el motivo "Figura en el Clearing de Informes: -30" usa U+002D y ningún motivo contiene U+2212

#### Scenario: Caracteres no ASCII en UTF-8
- **WHEN** se envía POST /evaluar con la solicitud base, `cuota_deudas` 40000 y `edad` 16
- **THEN** responde 200 y los bytes de la respuesta decodificados como UTF-8 contienen "Carga de deuda de más del 50% del ingreso: -15", "Antigüedad" y "No elegible: menor de 18 años"

#### Scenario: Motivo de elegibilidad sin puntaje
- **WHEN** se envía POST /evaluar con la solicitud base
- **THEN** responde 200 y el primer motivo es exactamente "Edad de 18 años o más: elegible", sin ": 0" ni otro puntaje

#### Scenario: Error sin textos de motivos
- **WHEN** se envía POST /evaluar con la solicitud base y `documentacion` null
- **THEN** responde 400 `validacion` sin `motivos`

## ADDED Requirements

### Requirement: Elegibilidad por edad
Una solicitud con `edad` menor que la edad mínima configurada (por defecto 18)
SHALL ser no elegible. Toda respuesta 200 MUST incluir como primer motivo
"Edad de <mínima> años o más: elegible" o "No elegible: menor de <mínima>
años". Las reglas de puntaje se evalúan igual para una solicitud no elegible
y sus motivos aparecen como para una elegible.

#### Scenario: Edad 17 no elegible
- **WHEN** se envía POST /evaluar con la solicitud base y `edad` 17
- **THEN** responde 200 con `prioridad` "BAJA", `score` 0 y el primer motivo "No elegible: menor de 18 años"

#### Scenario: Edad 18 elegible
- **WHEN** se envía POST /evaluar con la solicitud base y `edad` 18
- **THEN** responde 200 con `prioridad` "ALTA", `score` 85 y el primer motivo "Edad de 18 años o más: elegible"

#### Scenario: Edad 1 no elegible
- **WHEN** se envía POST /evaluar con la solicitud base y `edad` 1
- **THEN** responde 200 con `prioridad` "BAJA", `score` 0 y el primer motivo "No elegible: menor de 18 años"

#### Scenario: Edad 100 elegible
- **WHEN** se envía POST /evaluar con la solicitud base y `edad` 100
- **THEN** responde 200 con `score` 85 y el primer motivo "Edad de 18 años o más: elegible"

#### Scenario: Menor con perfil de prioridad alta
- **WHEN** se envía POST /evaluar con la solicitud base y `edad` 16
- **THEN** responde 200 con `prioridad` "BAJA", `score` 0 y los 7 motivos de puntaje de la solicitud base, en el mismo orden y con los mismos puntos

#### Scenario: Edad cero no se evalúa
- **WHEN** se envía POST /evaluar con la solicitud base y `edad` 0
- **THEN** responde 400 `validacion` con `fuera-de-rango` en `edad` y sin `motivos`

### Requirement: Ajuste por inelegibilidad
Para una solicitud no elegible, el score SHALL ser 0 y la prioridad BAJA, sin
importar la suma ni los umbrales de prioridad. Siempre se agrega al final un
único motivo "Score ajustado a 0 por inelegibilidad (suma <suma>)", también
si la suma ya es 0 o está entre 0 y 100, en lugar de cualquier motivo de
recorte.

#### Scenario: Menor con suma entre 0 y 100
- **WHEN** se envía POST /evaluar con la solicitud base y `edad` 16
- **THEN** responde 200 con `score` 0, `prioridad` "BAJA" y el último motivo "Score ajustado a 0 por inelegibilidad (suma 85)"

#### Scenario: Menor con suma 0
- **WHEN** se envía POST /evaluar con la solicitud del escenario "Suma exactamente 0" y `edad` 16
- **THEN** responde 200 con `score` 0 y el último motivo "Score ajustado a 0 por inelegibilidad (suma 0)"

#### Scenario: Menor con suma negativa
- **WHEN** se envía POST /evaluar con la solicitud del escenario "Suma negativa" y `edad` 16
- **THEN** responde 200 con `score` 0, el último motivo "Score ajustado a 0 por inelegibilidad (suma -55)" y ningún motivo "Score limitado"

#### Scenario: Menor con suma mayor que 100
- **WHEN** con una configuración de prueba en la que la solicitud base suma 105, se envía POST /evaluar con la solicitud base y `edad` 16
- **THEN** responde 200 con `score` 0, `prioridad` "BAJA", el último motivo "Score ajustado a 0 por inelegibilidad (suma 105)" y ningún motivo "Score limitado"

#### Scenario: Mayor de edad con suma negativa
- **WHEN** se envía POST /evaluar con la solicitud del escenario "Suma negativa"
- **THEN** responde 200 con el último motivo "Score limitado al mínimo de 0 (suma -55)" y ningún motivo "Score ajustado"

#### Scenario: Menor con prioridad media configurada en 0
- **WHEN** con una configuración de prueba en la que el umbral de MEDIA es 0, se envía POST /evaluar con la solicitud base y `edad` 16
- **THEN** responde 200 con `score` 0 y `prioridad` "BAJA"

#### Scenario: Solicitud inválida sin ajuste
- **WHEN** se envía POST /evaluar con la solicitud base, `edad` 16 y sin `clearing`
- **THEN** responde 400 `validacion` con `requerido` en `clearing` y sin `motivos`

### Requirement: Edad mínima configurable
La edad mínima SHALL configurarse fuera del código como un entero de 1 a 100 y
validarse al arrancar. Los textos de elegibilidad MUST usar el valor
configurado. Si falta, tiene decimales o está fuera de rango, la aplicación
MUST NOT arrancar.

#### Scenario: Edad mínima 21 con edad 20
- **WHEN** la aplicación arranca con la edad mínima en 21 y se envía POST /evaluar con la solicitud base y `edad` 20
- **THEN** responde 200 con `score` 0 y el primer motivo "No elegible: menor de 21 años"

#### Scenario: Edad mínima 21 con edad 21
- **WHEN** la aplicación arranca con la edad mínima en 21 y se envía POST /evaluar con la solicitud base y `edad` 21
- **THEN** responde 200 con `score` 85 y el primer motivo "Edad de 21 años o más: elegible"

#### Scenario: Edad mínima faltante
- **WHEN** la aplicación arranca sin la edad mínima configurada
- **THEN** la aplicación no arranca y POST /evaluar no recibe respuesta

#### Scenario: Edad mínima 0
- **WHEN** la aplicación arranca con la edad mínima en 0
- **THEN** la aplicación no arranca y POST /evaluar no recibe respuesta

#### Scenario: Edad mínima 101
- **WHEN** la aplicación arranca con la edad mínima en 101
- **THEN** la aplicación no arranca y POST /evaluar no recibe respuesta

#### Scenario: Edad mínima con decimales
- **WHEN** la aplicación arranca con la edad mínima en 18.5
- **THEN** la aplicación no arranca y POST /evaluar no recibe respuesta
