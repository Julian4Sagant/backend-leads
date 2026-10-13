# Spec Delta

## Purpose

Evaluar una solicitud de préstamo ya normalizada por n8n y devolver su prioridad
(ALTA, MEDIA o BAJA) con un score, los motivos legibles que lo explican y la
versión de las reglas aplicadas, de forma determinística y auditable.

## Convenciones

- **Solicitud base:** el JSON con datos ficticios del escenario "Solicitud
  base evaluada". Los demás escenarios indican solo los campos que cambian
  respecto de ella. Su resultado es score 85 y prioridad ALTA.
- Todas las peticiones de los escenarios llevan una X-API-Key válida, salvo
  que el escenario diga otra cosa.
- "400 `validacion` con `<codigo>` en `<campo>`" significa: estado 400, `type`
  `urn:leads-api:error:validacion` y un error con ese `campo` y ese `codigo`
  en la lista `errores`.

## ADDED Requirements

### Requirement: Evaluación de una solicitud válida
El sistema SHALL aceptar en POST /evaluar un objeto JSON con los 12 campos de
la solicitud base y responder 200 con `prioridad`, `score`, `motivos` y
`version_reglas`. Los campos que no forman parte del contrato se ignoran.

#### Scenario: Solicitud base evaluada
- **WHEN** se envía POST /evaluar con la solicitud base, que es este JSON: `{"session_id": "beb55e7eb0c947ff86f19aa0e0d0b2db", "edad": 32, "motivo": "auto", "monto": 150000, "plazo_meses": 24, "urgencia": "esta_semana", "situacion_laboral": "dependiente", "antiguedad_meses": 36, "ingreso_mensual": 60000, "cuota_deudas": 5000, "clearing": "no", "documentacion": "si"}`
- **THEN** responde 200 con `prioridad` "ALTA", `score` 85, 7 motivos y un `version_reglas`

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
- **WHEN** se envía POST /evaluar con la solicitud base, `edad` 13 y `clearing` "talvez"
- **THEN** responde 400 `validacion` con exactamente 2 errores: `fuera-de-rango` en `edad` y `valor-no-permitido` en `clearing`

### Requirement: Valores permitidos de los códigos
Los campos de código SHALL aceptar solo estos valores exactos, en minúscula y
sin espacios: `motivo` auto, vivienda, deudas, negocio, estudio, salud, viaje,
otro; `urgencia` esta_semana, este_mes, 1_a_3_meses, sin_apuro;
`situacion_laboral` dependiente, independiente, jubilado, desempleado,
estudiante; `clearing` si, no, no_se; `documentacion` si, no.

#### Scenario: Código válido
- **WHEN** se envía POST /evaluar con la solicitud base y `urgencia` "1_a_3_meses"
- **THEN** responde 200

#### Scenario: Código desconocido
- **WHEN** se envía POST /evaluar con la solicitud base y `urgencia` "pronto"
- **THEN** responde 400 `validacion` con `valor-no-permitido` en `urgencia`

#### Scenario: Código con mayúscula
- **WHEN** se envía POST /evaluar con la solicitud base y `motivo` "Auto"
- **THEN** responde 400 `validacion` con `valor-no-permitido` en `motivo`

#### Scenario: Código con espacios
- **WHEN** se envía POST /evaluar con la solicitud base y `motivo` " auto"
- **THEN** responde 400 `validacion` con `valor-no-permitido` en `motivo`

#### Scenario: Código con tipo incorrecto
- **WHEN** se envía POST /evaluar con la solicitud base y `urgencia` 5
- **THEN** responde 400 `validacion` con `tipo-invalido` en `urgencia`

### Requirement: Formato de los números
Los campos numéricos SHALL aceptarse como número JSON o como string numérico
con el mismo valor. Un booleano, array u objeto MUST dar `tipo-invalido`. Un
texto que no es un número decimal, un entero con parte decimal o un importe
con más de 2 decimales o más de 12 dígitos enteros MUST dar
`formato-invalido`, sin redondear ni truncar. Los enteros son `edad`,
`plazo_meses` y `antiguedad_meses`; los importes, `monto`, `ingreso_mensual`
y `cuota_deudas`.

#### Scenario: Número como string
- **WHEN** se envía POST /evaluar con la solicitud base y `monto` "150000"
- **THEN** responde 200 con el mismo cuerpo que la solicitud base

#### Scenario: Importe con dos decimales
- **WHEN** se envía POST /evaluar con la solicitud base y `monto` 150000.00
- **THEN** responde 200 con el mismo cuerpo que la solicitud base

#### Scenario: Importe con doce dígitos enteros
- **WHEN** se envía POST /evaluar con la solicitud base y `ingreso_mensual` 999999999999.99
- **THEN** responde 200

#### Scenario: Importe con trece dígitos enteros
- **WHEN** se envía POST /evaluar con la solicitud base y `ingreso_mensual` 1000000000000
- **THEN** responde 400 `validacion` con `formato-invalido` en `ingreso_mensual`

#### Scenario: Importe con tres decimales
- **WHEN** se envía POST /evaluar con la solicitud base y `monto` 200.555
- **THEN** responde 400 `validacion` con `formato-invalido` en `monto`

#### Scenario: Entero con parte decimal
- **WHEN** se envía POST /evaluar con la solicitud base y `plazo_meses` 12.5
- **THEN** responde 400 `validacion` con `formato-invalido` en `plazo_meses`

#### Scenario: Texto no numérico
- **WHEN** se envía POST /evaluar con la solicitud base y `monto` "abc"
- **THEN** responde 400 `validacion` con `formato-invalido` en `monto`

#### Scenario: String vacío en un número
- **WHEN** se envía POST /evaluar con la solicitud base y `monto` ""
- **THEN** responde 400 `validacion` con `formato-invalido` en `monto`

#### Scenario: Booleano en un número
- **WHEN** se envía POST /evaluar con la solicitud base y `plazo_meses` true
- **THEN** responde 400 `validacion` con `tipo-invalido` en `plazo_meses`

#### Scenario: Array en un número
- **WHEN** se envía POST /evaluar con la solicitud base y `monto` []
- **THEN** responde 400 `validacion` con `tipo-invalido` en `monto`

### Requirement: Rangos de los números
Los números SHALL cumplir estos rangos, con los extremos incluidos: `monto`
mayor que 0; `ingreso_mensual` y `cuota_deudas` mayores o iguales a 0;
`plazo_meses` de 1 a 120; `edad` de 14 a 100; `antiguedad_meses` de 0 a 600.
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

#### Scenario: Edad 13
- **WHEN** se envía POST /evaluar con la solicitud base y `edad` 13
- **THEN** responde 400 `validacion` con `fuera-de-rango` en `edad`

#### Scenario: Edad 14
- **WHEN** se envía POST /evaluar con la solicitud base y `edad` 14
- **THEN** responde 200

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

### Requirement: Formato de session_id
`session_id` SHALL ser un string de 1 a 64 caracteres ASCII, solo letras,
dígitos, guion y guion bajo (`^[A-Za-z0-9_-]{1,64}$`). Si no cumple, MUST dar
`formato-invalido`.

#### Scenario: session_id de 64 caracteres
- **WHEN** se envía POST /evaluar con la solicitud base y un `session_id` de 64 caracteres válidos
- **THEN** responde 200

#### Scenario: session_id de 65 caracteres
- **WHEN** se envía POST /evaluar con la solicitud base y un `session_id` de 65 caracteres válidos
- **THEN** responde 400 `validacion` con `formato-invalido` en `session_id`

#### Scenario: session_id vacío
- **WHEN** se envía POST /evaluar con la solicitud base y `session_id` ""
- **THEN** responde 400 `validacion` con `formato-invalido` en `session_id`

#### Scenario: session_id con carácter no ASCII
- **WHEN** se envía POST /evaluar con la solicitud base y `session_id` "sesión1"
- **THEN** responde 400 `validacion` con `formato-invalido` en `session_id`

#### Scenario: session_id con salto de línea
- **WHEN** se envía POST /evaluar con la solicitud base y `session_id` "abc\ndef"
- **THEN** responde 400 `validacion` con `formato-invalido` en `session_id`

### Requirement: Regla de urgencia
La regla "Urgencia" SHALL sumar esta_semana +20, este_mes +15, 1_a_3_meses +5
y sin_apuro 0, con los motivos "Urgencia esta semana: +20", "Urgencia este
mes: +15", "Urgencia de 1 a 3 meses: +5" y "Sin urgencia: 0".

#### Scenario: Urgencia esta semana
- **WHEN** se envía POST /evaluar con la solicitud base
- **THEN** responde 200 y `motivos` incluye "Urgencia esta semana: +20"

#### Scenario: Urgencia este mes
- **WHEN** se envía POST /evaluar con la solicitud base y `urgencia` "este_mes"
- **THEN** responde 200 con `score` 80 y `motivos` incluye "Urgencia este mes: +15"

#### Scenario: Urgencia de 1 a 3 meses
- **WHEN** se envía POST /evaluar con la solicitud base y `urgencia` "1_a_3_meses"
- **THEN** responde 200 con `score` 70 y `motivos` incluye "Urgencia de 1 a 3 meses: +5"

#### Scenario: Sin urgencia
- **WHEN** se envía POST /evaluar con la solicitud base y `urgencia` "sin_apuro"
- **THEN** responde 200 con `score` 65 y `motivos` incluye "Sin urgencia: 0"

#### Scenario: Urgencia inválida no se evalúa
- **WHEN** se envía POST /evaluar con la solicitud base y `urgencia` "ya"
- **THEN** responde 400 `validacion` con `valor-no-permitido` en `urgencia` y sin `motivos`

### Requirement: Regla de documentación
La regla "Documentación" SHALL sumar +15 si `documentacion` es si y 0 si es
no, con los motivos "Documentación de ingresos disponible: +15" y "Sin
documentación de ingresos: 0". `documentacion` se refiere a documentación de
ingresos.

#### Scenario: Con documentación
- **WHEN** se envía POST /evaluar con la solicitud base
- **THEN** responde 200 y `motivos` incluye "Documentación de ingresos disponible: +15"

#### Scenario: Sin documentación
- **WHEN** se envía POST /evaluar con la solicitud base y `documentacion` "no"
- **THEN** responde 200 con `score` 70 y `motivos` incluye "Sin documentación de ingresos: 0"

#### Scenario: Documentación inválida
- **WHEN** se envía POST /evaluar con la solicitud base y `documentacion` "tal_vez"
- **THEN** responde 400 `validacion` con `valor-no-permitido` en `documentacion`

### Requirement: Regla de destino del préstamo
La regla "Destino del préstamo" SHALL sumar +5 si `motivo` es distinto de otro
y 0 si es otro, con los motivos "Destino del préstamo definido: +5" y
"Destino del préstamo no definido: 0". Los textos dicen "Destino del préstamo"
para no confundir el campo `motivo` con la lista `motivos`.

#### Scenario: Destino definido
- **WHEN** se envía POST /evaluar con la solicitud base
- **THEN** responde 200 y `motivos` incluye "Destino del préstamo definido: +5"

#### Scenario: Destino no definido
- **WHEN** se envía POST /evaluar con la solicitud base y `motivo` "otro"
- **THEN** responde 200 con `score` 80 y `motivos` incluye "Destino del préstamo no definido: 0"

#### Scenario: Destino inválido
- **WHEN** se envía POST /evaluar con la solicitud base y `motivo` "casa"
- **THEN** responde 400 `validacion` con `valor-no-permitido` en `motivo`

### Requirement: Regla de situación laboral
La regla "Situación laboral" SHALL sumar dependiente +15, independiente +10,
jubilado +15, estudiante 0 y desempleado -10, con los motivos "Empleado en
relación de dependencia: +15", "Trabajador independiente: +10", "Jubilado:
+15", "Estudiante: 0" y "Desempleado: -10".

#### Scenario: Dependiente
- **WHEN** se envía POST /evaluar con la solicitud base
- **THEN** responde 200 y `motivos` incluye "Empleado en relación de dependencia: +15"

#### Scenario: Independiente
- **WHEN** se envía POST /evaluar con la solicitud base y `situacion_laboral` "independiente"
- **THEN** responde 200 con `score` 80 y `motivos` incluye "Trabajador independiente: +10"

#### Scenario: Jubilado
- **WHEN** se envía POST /evaluar con la solicitud base, `situacion_laboral` "jubilado" y `antiguedad_meses` 0
- **THEN** responde 200 con `score` 75 y `motivos` incluye "Jubilado: +15"

#### Scenario: Estudiante
- **WHEN** se envía POST /evaluar con la solicitud base, `situacion_laboral` "estudiante" y `antiguedad_meses` 0
- **THEN** responde 200 con `score` 60 y `motivos` incluye "Estudiante: 0"

#### Scenario: Desempleado
- **WHEN** se envía POST /evaluar con la solicitud base, `situacion_laboral` "desempleado" y `antiguedad_meses` 0
- **THEN** responde 200 con `score` 50 y `motivos` incluye "Desempleado: -10"

#### Scenario: Situación laboral inválida
- **WHEN** se envía POST /evaluar con la solicitud base y `situacion_laboral` "monotributista"
- **THEN** responde 400 `validacion` con `valor-no-permitido` en `situacion_laboral`

### Requirement: Regla de antigüedad laboral
Para dependiente o independiente, la regla "Antigüedad" SHALL sumar +10 con 12
meses o más, +5 de 6 a 11 meses y 0 con menos de 6, con los motivos
"Antigüedad laboral de 12 meses o más: +10", "Antigüedad laboral de 6 a 11
meses: +5" y "Antigüedad laboral de menos de 6 meses: 0". Para jubilado,
desempleado o estudiante SHALL sumar 0 con "Antigüedad laboral no aplica a
<situacion_laboral>: 0".

#### Scenario: Antigüedad de 12 meses
- **WHEN** se envía POST /evaluar con la solicitud base y `antiguedad_meses` 12
- **THEN** responde 200 con `score` 85 y `motivos` incluye "Antigüedad laboral de 12 meses o más: +10"

#### Scenario: Antigüedad de 11 meses
- **WHEN** se envía POST /evaluar con la solicitud base y `antiguedad_meses` 11
- **THEN** responde 200 con `score` 80 y `motivos` incluye "Antigüedad laboral de 6 a 11 meses: +5"

#### Scenario: Antigüedad de 6 meses
- **WHEN** se envía POST /evaluar con la solicitud base y `antiguedad_meses` 6
- **THEN** responde 200 con `score` 80 y `motivos` incluye "Antigüedad laboral de 6 a 11 meses: +5"

#### Scenario: Antigüedad de 5 meses
- **WHEN** se envía POST /evaluar con la solicitud base y `antiguedad_meses` 5
- **THEN** responde 200 con `score` 75 y `motivos` incluye "Antigüedad laboral de menos de 6 meses: 0"

#### Scenario: Antigüedad de independiente
- **WHEN** se envía POST /evaluar con la solicitud base, `situacion_laboral` "independiente" y `antiguedad_meses` 12
- **THEN** responde 200 y `motivos` incluye "Antigüedad laboral de 12 meses o más: +10"

#### Scenario: Antigüedad no aplica a jubilado
- **WHEN** se envía POST /evaluar con la solicitud base, `situacion_laboral` "jubilado" y `antiguedad_meses` 600
- **THEN** responde 200 con `score` 75 y `motivos` incluye "Antigüedad laboral no aplica a jubilado: 0"

#### Scenario: Antigüedad no aplica a desempleado
- **WHEN** se envía POST /evaluar con la solicitud base, `situacion_laboral` "desempleado" y `antiguedad_meses` 0
- **THEN** responde 200 y `motivos` incluye "Antigüedad laboral no aplica a desempleado: 0"

#### Scenario: Antigüedad no aplica a estudiante
- **WHEN** se envía POST /evaluar con la solicitud base, `situacion_laboral` "estudiante" y `antiguedad_meses` 0
- **THEN** responde 200 y `motivos` incluye "Antigüedad laboral no aplica a estudiante: 0"

#### Scenario: Antigüedad que no aplica se valida igual
- **WHEN** se envía POST /evaluar con la solicitud base, `situacion_laboral` "jubilado" y `antiguedad_meses` 601
- **THEN** responde 400 `validacion` con `fuera-de-rango` en `antiguedad_meses`

### Requirement: Regla de clearing
La regla "Clearing" SHALL sumar si -30, no_se -5 y no 0, con los motivos
"Figura en el Clearing de Informes: -30", "No sabe si figura en el Clearing
de Informes: -5" y "No figura en el Clearing de Informes: 0". "si" significa
que la persona figura en el Clearing de Informes por deudas impagas.

#### Scenario: No figura en el Clearing
- **WHEN** se envía POST /evaluar con la solicitud base
- **THEN** responde 200 y `motivos` incluye "No figura en el Clearing de Informes: 0"

#### Scenario: Figura en el Clearing
- **WHEN** se envía POST /evaluar con la solicitud base y `clearing` "si"
- **THEN** responde 200 con `score` 55, `prioridad` "MEDIA" y `motivos` incluye "Figura en el Clearing de Informes: -30"

#### Scenario: No sabe si figura en el Clearing
- **WHEN** se envía POST /evaluar con la solicitud base y `clearing` "no_se"
- **THEN** responde 200 con `score` 80 y `motivos` incluye "No sabe si figura en el Clearing de Informes: -5"

#### Scenario: Clearing inválido
- **WHEN** se envía POST /evaluar con la solicitud base y `clearing` "No"
- **THEN** responde 400 `validacion` con `valor-no-permitido` en `clearing`

### Requirement: Regla de carga de deuda
La regla "Carga" SHALL calcular carga = (cuota_deudas + monto / plazo_meses) /
ingreso_mensual, con la cuota estimada sin intereses, y sumar +20 si carga <=
0,3, +5 si 0,3 < carga <= 0,5 y -15 si carga > 0,5, con los textos de la
decisión 5. Un ingreso 0 MUST caer en el último tramo. Los bordes se comparan
sin redondeo.

#### Scenario: Carga exactamente 0,3
- **WHEN** se envía POST /evaluar con la solicitud base, `monto` 24000, `plazo_meses` 24, `ingreso_mensual` 10000 y `cuota_deudas` 2000
- **THEN** responde 200 y `motivos` incluye "Carga de deuda hasta el 30% del ingreso: +20"

#### Scenario: Carga apenas mayor que 0,3
- **WHEN** se envía POST /evaluar con la solicitud base, `monto` 24000, `plazo_meses` 24, `ingreso_mensual` 10000 y `cuota_deudas` 2000.01
- **THEN** responde 200 y `motivos` incluye "Carga de deuda de más del 30% y hasta el 50% del ingreso: +5"

#### Scenario: Carga exactamente 0,5
- **WHEN** se envía POST /evaluar con la solicitud base, `monto` 24000, `plazo_meses` 24, `ingreso_mensual` 10000 y `cuota_deudas` 4000
- **THEN** responde 200 y `motivos` incluye "Carga de deuda de más del 30% y hasta el 50% del ingreso: +5"

#### Scenario: Carga apenas mayor que 0,5
- **WHEN** se envía POST /evaluar con la solicitud base, `monto` 24000, `plazo_meses` 24, `ingreso_mensual` 10000 y `cuota_deudas` 4000.01
- **THEN** responde 200 y `motivos` incluye "Carga de deuda de más del 50% del ingreso: -15"

#### Scenario: Ingreso cero cae en el tramo más alto
- **WHEN** se envía POST /evaluar con la solicitud base, `ingreso_mensual` 0 y `cuota_deudas` 0
- **THEN** responde 200 y `motivos` incluye "Carga de deuda de más del 50% del ingreso: -15"

#### Scenario: Carga sin plazo válido
- **WHEN** se envía POST /evaluar con la solicitud base y `plazo_meses` 0
- **THEN** responde 400 `validacion` con `fuera-de-rango` en `plazo_meses`, sin calcular la carga

### Requirement: Score y recorte
El score SHALL ser max(0, min(100, suma de los puntos de los motivos de
puntaje)). Si se aplica un recorte, se agrega al final un motivo sin puntos:
"Score limitado al mínimo de 0 (suma <suma>)" o "Score limitado al máximo de
100 (suma <suma>)". Una suma de exactamente 0 o 100 MUST NOT agregar motivo
de recorte.

#### Scenario: Suma negativa
- **WHEN** se envía POST /evaluar con la solicitud base, `situacion_laboral` "desempleado", `antiguedad_meses` 0, `urgencia` "sin_apuro", `documentacion` "no", `motivo` "otro", `clearing` "si" y `cuota_deudas` 60000
- **THEN** responde 200 con `score` 0, `prioridad` "BAJA" y el último motivo "Score limitado al mínimo de 0 (suma -55)"

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

### Requirement: Prioridad según el score
La prioridad SHALL ser ALTA con score >= 60, MEDIA con score de 30 a 59 y BAJA
con score < 30. Con los puntajes actuales todos los scores son múltiplos de 5,
así que los bordes 59 y 29 se prueban con una configuración de prueba.

#### Scenario: Score 60
- **WHEN** se envía POST /evaluar con la solicitud base, `antiguedad_meses` 3 y `cuota_deudas` 15000
- **THEN** responde 200 con `score` 60 y `prioridad` "ALTA"

#### Scenario: Score 59
- **WHEN** con una configuración de prueba en la que la solicitud base suma 59, se envía POST /evaluar con la solicitud base
- **THEN** responde 200 con `score` 59 y `prioridad` "MEDIA"

#### Scenario: Score 30
- **WHEN** se envía POST /evaluar con la solicitud base, `situacion_laboral` "estudiante", `antiguedad_meses` 0, `urgencia` "1_a_3_meses" y `documentacion` "no"
- **THEN** responde 200 con `score` 30 y `prioridad` "MEDIA"

#### Scenario: Score 29
- **WHEN** con una configuración de prueba en la que la solicitud base suma 29, se envía POST /evaluar con la solicitud base
- **THEN** responde 200 con `score` 29 y `prioridad` "BAJA"

#### Scenario: Solicitud inválida sin prioridad
- **WHEN** se envía POST /evaluar con la solicitud base y `clearing` "quizas"
- **THEN** responde 400 `validacion` sin `prioridad`

### Requirement: Orden y forma de los motivos
`motivos` SHALL listar exactamente un motivo por regla de puntaje, también
cuando suma 0, en este orden: urgencia, documentación, destino del préstamo,
situación laboral, antigüedad, clearing, carga; y al final, si corresponde, un
único motivo de recorte: 7 motivos, u 8 con recorte. La suma de los puntos de
los motivos de puntaje MUST reproducir la suma antes del recorte. El change
`leads-api-agregar-elegibilidad-edad` agrega el motivo excluyente al
principio.

#### Scenario: Orden completo de la solicitud base
- **WHEN** se envía POST /evaluar con la solicitud base
- **THEN** responde 200 con `motivos` exactamente: "Urgencia esta semana: +20", "Documentación de ingresos disponible: +15", "Destino del préstamo definido: +5", "Empleado en relación de dependencia: +15", "Antigüedad laboral de 12 meses o más: +10", "No figura en el Clearing de Informes: 0", "Carga de deuda hasta el 30% del ingreso: +20"

#### Scenario: Motivo de recorte al final y una sola vez
- **WHEN** se envía POST /evaluar con la solicitud del escenario "Suma negativa"
- **THEN** responde 200 con 8 motivos, el motivo de recorte es el último y aparece una sola vez

#### Scenario: Error sin motivos
- **WHEN** se envía POST /evaluar con la solicitud base sin `urgencia`
- **THEN** responde 400 `validacion` sin `motivos`

### Requirement: Formato de los textos de los motivos
El puntaje en cada motivo SHALL escribirse `+N` si es positivo, `-N` si es
negativo y `0` sin signo, con el guion ASCII (U+002D) y nunca el signo menos
Unicode (U+2212). Los textos se envían en UTF-8.

#### Scenario: Puntaje negativo con guion ASCII
- **WHEN** se envía POST /evaluar con la solicitud base y `clearing` "si"
- **THEN** responde 200 y el motivo "Figura en el Clearing de Informes: -30" usa U+002D y ningún motivo contiene U+2212

#### Scenario: Caracteres no ASCII en UTF-8
- **WHEN** se envía POST /evaluar con la solicitud base y `cuota_deudas` 40000
- **THEN** responde 200 y los bytes de la respuesta decodificados como UTF-8 contienen "Carga de deuda de más del 50% del ingreso: -15" y "Antigüedad"

#### Scenario: Error sin textos de motivos
- **WHEN** se envía POST /evaluar con la solicitud base y `documentacion` null
- **THEN** responde 400 `validacion` sin `motivos`

### Requirement: Forma de la respuesta exitosa
La respuesta 200 SHALL ser un objeto con exactamente `prioridad` (ALTA, MEDIA
o BAJA), `score` (entero de 0 a 100), `motivos` (array de strings) y
`version_reglas`. El Content-Type y el charset se definen en la spec
`acceso-api`.

#### Scenario: Claves de la respuesta exitosa
- **WHEN** se envía POST /evaluar con la solicitud base
- **THEN** responde 200 con solo las claves `prioridad`, `score`, `motivos` y `version_reglas`, y `score` es un número entero

#### Scenario: Respuesta de error sin claves de evaluación
- **WHEN** se envía POST /evaluar con la solicitud base sin `edad`
- **THEN** responde 400 sin las claves `prioridad`, `score`, `motivos` ni `version_reglas`

### Requirement: Determinismo
La misma solicitud SHALL producir siempre la misma respuesta con la misma
configuración, sin estado entre llamadas. Un reintento de n8n MUST recibir la
misma respuesta.

#### Scenario: Reintento con la misma solicitud
- **WHEN** se envía dos veces POST /evaluar con la solicitud base
- **THEN** las dos respuestas son 200 con cuerpos idénticos

#### Scenario: Reintento de una solicitud inválida
- **WHEN** se envía dos veces POST /evaluar con la solicitud base y `edad` 101
- **THEN** las dos respuestas son 400 con cuerpos idénticos

### Requirement: Tabla de máximos por situación laboral
Con la configuración efectiva, el score máximo alcanzable SHALL coincidir con
esta tabla, que no se normaliza a 100 (máximo sin clearing / con clearing
si): dependiente 85 / 55; independiente 80 / 50; jubilado 75 / 45;
estudiante 60 / 30; desempleado 50 / 20. Con clearing si, nadie llega a ALTA.

#### Scenario: Máximo de dependiente
- **WHEN** se evalúan por POST /evaluar todas las combinaciones de códigos y tramos con `situacion_laboral` "dependiente" y `clearing` distinto de "si"
- **THEN** todas responden 200 y el score máximo es 85 (ALTA)

#### Scenario: Máximo de dependiente con clearing
- **WHEN** se evalúan por POST /evaluar todas las combinaciones de códigos y tramos con `situacion_laboral` "dependiente" y `clearing` "si"
- **THEN** todas responden 200 y el score máximo es 55 (MEDIA)

#### Scenario: Máximo de independiente
- **WHEN** se evalúan por POST /evaluar todas las combinaciones de códigos y tramos con `situacion_laboral` "independiente" y `clearing` distinto de "si"
- **THEN** todas responden 200 y el score máximo es 80 (ALTA)

#### Scenario: Máximo de independiente con clearing
- **WHEN** se evalúan por POST /evaluar todas las combinaciones de códigos y tramos con `situacion_laboral` "independiente" y `clearing` "si"
- **THEN** todas responden 200 y el score máximo es 50 (MEDIA)

#### Scenario: Máximo de jubilado
- **WHEN** se evalúan por POST /evaluar todas las combinaciones de códigos y tramos con `situacion_laboral` "jubilado" y `clearing` distinto de "si"
- **THEN** todas responden 200 y el score máximo es 75 (ALTA)

#### Scenario: Máximo de jubilado con clearing
- **WHEN** se evalúan por POST /evaluar todas las combinaciones de códigos y tramos con `situacion_laboral` "jubilado" y `clearing` "si"
- **THEN** todas responden 200 y el score máximo es 45 (MEDIA)

#### Scenario: Máximo de estudiante
- **WHEN** se evalúan por POST /evaluar todas las combinaciones de códigos y tramos con `situacion_laboral` "estudiante" y `clearing` distinto de "si"
- **THEN** todas responden 200 y el score máximo es 60 (ALTA)

#### Scenario: Máximo de estudiante con clearing
- **WHEN** se evalúan por POST /evaluar todas las combinaciones de códigos y tramos con `situacion_laboral` "estudiante" y `clearing` "si"
- **THEN** todas responden 200 y el score máximo es 30 (MEDIA)

#### Scenario: Máximo de desempleado
- **WHEN** se evalúan por POST /evaluar todas las combinaciones de códigos y tramos con `situacion_laboral` "desempleado" y `clearing` distinto de "si"
- **THEN** todas responden 200 y el score máximo es 50 (MEDIA)

#### Scenario: Máximo de desempleado con clearing
- **WHEN** se evalúan por POST /evaluar todas las combinaciones de códigos y tramos con `situacion_laboral` "desempleado" y `clearing` "si"
- **THEN** todas responden 200 y el score máximo es 20 (BAJA)

#### Scenario: Máximo de estudiante en el borde de ALTA
- **WHEN** se envía POST /evaluar con la solicitud base, `situacion_laboral` "estudiante" y `antiguedad_meses` 0
- **THEN** responde 200 con `score` 60 y `prioridad` "ALTA"

#### Scenario: Máximo con clearing no llega a ALTA
- **WHEN** se envía POST /evaluar con la solicitud base y `clearing` "si"
- **THEN** responde 200 con `score` 55 y `prioridad` "MEDIA"

#### Scenario: Combinación inválida fuera de la tabla
- **WHEN** se envía POST /evaluar con la solicitud base y `situacion_laboral` "jubilada"
- **THEN** responde 400 `validacion` con `valor-no-permitido` en `situacion_laboral`

### Requirement: Configuración de reglas validada al arrancar
Puntajes y umbrales SHALL configurarse fuera del código y validarse al
arrancar. Si la configuración es incoherente (umbral de ALTA no mayor que el
de MEDIA, umbral bajo de carga no menor que el alto, un valor faltante), la
aplicación MUST NOT arrancar.

#### Scenario: Configuración coherente
- **WHEN** la aplicación arranca con la configuración por defecto y se envía POST /evaluar con la solicitud base
- **THEN** responde 200

#### Scenario: Umbral de ALTA no mayor que el de MEDIA
- **WHEN** la aplicación arranca con el umbral de ALTA en 30 y el de MEDIA en 30
- **THEN** la aplicación no arranca y POST /evaluar no recibe respuesta

#### Scenario: Umbrales de carga invertidos
- **WHEN** la aplicación arranca con el umbral bajo de carga en 0,5 y el alto en 0,3
- **THEN** la aplicación no arranca y POST /evaluar no recibe respuesta

#### Scenario: Puntaje faltante
- **WHEN** la aplicación arranca sin el puntaje de urgencia esta_semana
- **THEN** la aplicación no arranca y POST /evaluar no recibe respuesta

### Requirement: Versión de las reglas
`version_reglas` SHALL ser 12 caracteres hexadecimales en minúscula que
identifican la configuración de puntajes y umbrales, las reglas y la revisión
de la lógica. Con la misma configuración y las mismas reglas MUST ser igual;
cualquier cambio de puntaje, umbral o regla MUST cambiarla.

#### Scenario: Formato de version_reglas
- **WHEN** se envía POST /evaluar con la solicitud base
- **THEN** responde 200 y `version_reglas` cumple `^[0-9a-f]{12}$`

#### Scenario: Misma configuración, misma versión
- **WHEN** se envía POST /evaluar con la solicitud base y con otra solicitud válida
- **THEN** las dos respuestas 200 tienen el mismo `version_reglas`

#### Scenario: Cambio de puntaje cambia la versión
- **WHEN** la aplicación arranca con el puntaje de documentación en 16 y se envía POST /evaluar con la solicitud base
- **THEN** responde 200 con un `version_reglas` distinto del de la configuración por defecto

#### Scenario: 20 y 20.0 dan la misma versión
- **WHEN** la aplicación arranca con el puntaje de urgencia esta_semana escrito como 20.0 y se envía POST /evaluar con la solicitud base
- **THEN** responde 200 con el mismo `version_reglas` que con 20

#### Scenario: Cambio de reglas cambia la versión
- **WHEN** la aplicación arranca con un archivo de reglas que tiene una regla agregada y se envía POST /evaluar con la solicitud base
- **THEN** responde 200 con un `version_reglas` distinto del de las reglas por defecto

#### Scenario: Finales de línea no cambian la versión
- **WHEN** la aplicación arranca con el mismo archivo de reglas con finales de línea CRLF y se envía POST /evaluar con la solicitud base
- **THEN** responde 200 con el mismo `version_reglas` que con LF

#### Scenario: Error sin versión
- **WHEN** se envía POST /evaluar con la solicitud base y `monto` 0
- **THEN** responde 400 `validacion` sin `version_reglas`
