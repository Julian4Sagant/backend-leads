# Proposal

## Change ID

`leads-api-agregar-elegibilidad-edad`

## Why

Hoy el servicio califica igual a un menor de edad que a un adulto: una persona
de 16 años puede salir con prioridad ALTA. Además, una edad de 1 a 13 años se
rechaza como error de validación, así que en la planilla un menor de 14 aparece
como `ERROR` en vez de como no elegible. Este change agrega la regla de
elegibilidad por edad prevista en el plan original (decisión 3).

## What Changes

- Nueva regla excluyente de edad: una solicitud con edad menor que la edad
  mínima es **no elegible**: prioridad BAJA y score 0. Las reglas de puntaje se
  evalúan igual y aparecen en los motivos.
- Nuevo motivo al principio de `motivos`, siempre: "Edad de 18 años o más:
  elegible" o "No elegible: menor de 18 años".
- Nuevo motivo final cuando la solicitud es no elegible: "Score ajustado a 0
  por inelegibilidad (suma X)". Reemplaza al motivo de recorte.
- Edad mínima configurable (hoy 18); los textos usan el valor configurado.
- El rango válido de `edad` pasa de 14 a 100 a **1 a 100**.
- `motivos` pasa a tener 8 elementos, o 9 con recorte o inelegibilidad.

## Contratos afectados

**POST /evaluar** (compatible con n8n):

| Campo | Dirección | Cambio |
| --- | --- | --- |
| `edad` | request | El rango válido pasa de 14 a 100 a **1 a 100**. Se aceptan más valores, nunca menos |
| `motivos` | response | Suma un primer elemento de elegibilidad y, para no elegibles, uno final de ajuste. Pasa de 7 a 8 elementos (9 con recorte o inelegibilidad) |
| `prioridad`, `score` | response | Para un menor de la edad mínima: siempre BAJA y 0 |
| `version_reglas` | response | Cambia de valor (cambian el DRL, la configuración y la lógica) |

No entran ni salen campos. n8n no necesita cambios: guarda `motivos` unidos en
una celda, sin depender de cuántos son.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `evaluacion-leads`: regla excluyente de edad, ajuste por inelegibilidad y su
  precedencia sobre el recorte, edad mínima configurable, rango de `edad` y
  orden y cantidad de motivos.

## Decisiones confirmadas

De la especificación inicial (decisiones 3 y 6, 2026-10-09):
- Si la edad es menor que la mínima: prioridad BAJA y score 0, con el motivo
  "No elegible: menor de 18 años". Las reglas de puntaje se evalúan igual y
  aparecen en los motivos.
- El ajuste por inelegibilidad reemplaza al recorte a 0 y 100: un solo motivo
  final, "Score ajustado a 0 por inelegibilidad (suma X)". Escenarios: menor
  con suma negativa, menor con suma mayor que 100 y mayor de edad con suma
  negativa.
- Las reglas excluyentes van primero en `motivos`, con su resultado también
  cuando pasan.

Del explore del change 2 (confirmadas por Julian, 2026-10-13):
- El motivo final de ajuste aparece **siempre** que la solicitud es no
  elegible, también si la suma ya es 0 o está entre 0 y 100.
- El rango válido de `edad` pasa a 1 a 100: un menor de 14 figura como no
  elegible y no como error. 0 o menos y más de 100 siguen siendo
  `fuera-de-rango`.
- Todos los menores llevan el mismo mensaje: "No elegible: menor de 18 años".
- La edad mínima se configura en `application.yml`, como el resto de los
  umbrales.
- Los textos usan la edad configurada: "Edad de 18 años o más: elegible"
  (reemplaza al "Mayor de edad: elegible" del plan original) y "No elegible:
  menor de 18 años".

Confirmadas por Julian en la revisión de la propuesta (2026-10-13):
- **Rango de la edad mínima configurada:** un entero de 1 a 100. Fuera de ese
  rango, o con decimales, la aplicación no arranca.
- **Los motivos de elegibilidad y de ajuste no llevan puntaje** al final
  (no dicen ": 0"), igual que el motivo de recorte del change 1. No suman al
  score.

Confirmadas por Julian durante la implementación (2026-10-13):
- Un menor de 18 no puede sacar un préstamo, y se lo muestra como **no
  elegible**, no como error: queda registrado que consultó y por qué se lo
  descartó.
- El escenario «Varios errores en una misma solicitud» usaba `edad` 13 como
  dato inválido, y con este change 13 es válido. Pasa a usar `edad` 0, el
  nuevo borde inválido; el resultado esperado no cambia.

## Supuestos pendientes

Ninguno.

## Fuera de alcance

- Que el chat de n8n avise a un menor desde el principio (anotado en el
  roadmap como pendiente fuera del plan).
- Otras reglas excluyentes además de la edad.
- Límite de tamaño del cuerpo y regex estrictas de números (changes 4 y 3).

## Estrategia de testing

- **Tabla de casos de la regla de edad** con los bordes exactos: 0 (error), 1,
  17, 18, 100 y 101 (error), más la edad mínima configurada en otro valor
  (21: 20 no elegible, 21 elegible) para probar que el texto la sigue.
- **Precedencia:** menor con suma negativa, menor con suma 0, menor con suma
  entre 0 y 100, menor con suma mayor que 100 (configuración de prueba) y mayor
  de edad con suma negativa. En cada caso, exactamente un motivo final.
- **Orden y conteo:** 8 motivos sin ajuste; 9 con recorte o inelegibilidad; el
  de edad siempre primero y el final una sola vez.
- **Configuración:** edad mínima faltante, 0, 101 o con decimales: no arranca.
- **Regresión:** se actualizan los tests del change 1 que contaban 7 motivos o
  probaban los bordes 13 y 14; el test de cierre de `version_reglas` registra
  las dos reglas nuevas con `REVISION_LOGICA` subida.

## Aprobadores requeridos

- Julian: aprueba la propuesta con un comentario en el PR y revisa el código.

## Impact

- Código: regla de edad en el DRL, motivos de tipo `ELEGIBILIDAD`, cálculo del
  score con la precedencia, textos, validación del rango de `edad`,
  configuración de la edad mínima.
- Configuración: nueva propiedad de edad mínima en `application.yml`.
- Tests: nuevos para la regla y la precedencia; se actualizan los que cuentan
  motivos o prueban el rango de `edad`.
- Sistemas: ninguno. n8n sigue funcionando sin cambios.
