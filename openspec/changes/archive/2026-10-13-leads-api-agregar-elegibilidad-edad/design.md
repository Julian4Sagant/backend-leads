# Design

## Context

El change 1 dejó el motor preparado para reglas excluyentes: el DRL agrupa
reglas por `@regla`, Java ordena los motivos con `CalculoEvaluacion.ORDEN_REGLAS`
y los textos salen de `TextosMotivos`. Hoy todos los motivos son de puntaje
(`Motivo(regla, codigo, puntos)`), `edad` se valida de 14 a 100 en
`ValidadorSolicitud` y no participa del cálculo. La motivación está en
proposal.md (Why); el comportamiento, en la spec `evaluacion-leads`.

## Goals / Non-Goals

**Goals:**
- Que la elegibilidad sea una regla del DRL más, separada de las de puntaje por
  el tipo de motivo, sin lógica de edad repartida en Java.
- Que la precedencia "inelegible antes que recorte" viva en un solo lugar.
- Dejar el camino abierto a otras reglas excluyentes sin rediseñar.

**Non-Goals:**
- Otras reglas excluyentes.
- Cortar la evaluación temprano para un no elegible: las reglas de puntaje se
  evalúan igual, por decisión de negocio.

## Endpoints

Sin cambios de forma: POST /evaluar con el mismo request y response (ver
spec `evaluacion-leads`). Cambian el rango válido de `edad` (1 a 100) y el
contenido de `motivos`, `score` y `prioridad` para un no elegible. Sin `type`
de error nuevos: una edad fuera de rango sigue siendo
`urn:leads-api:error:validacion` con `fuera-de-rango`.

## Decisions

### D1. Edad mínima en la configuración

Nueva propiedad `leads.reglas.umbrales.edad-minima: 18` en `application.yml`,
como componente `edadMinima` del record `ReglasProperties.Umbrales`, con
`@NotNull`, `@Entero`, `@DecimalMin("1")` y `@DecimalMax("100")`. Al estar en
`umbrales`, entra sola en `version_reglas` (se aplana por reflexión) y llega al
DRL por el global `umbrales` que ya existe.

- *Alternativa:* una sección `elegibilidad` aparte. Se descarta: es un umbral
  más y el global `umbrales` ya existe.

### D2. Motivos de tipo ELEGIBILIDAD

- Enum nuevo `TipoMotivo { ELEGIBILIDAD, PUNTAJE }` en `evaluacion/enums`.
- `Motivo` pasa a `Motivo(TipoMotivo tipo, String regla, String codigo, int puntos)`.
  Los de ELEGIBILIDAD tienen siempre 0 puntos y su código es `elegible` o
  `no-elegible` (constantes en `Motivo`).
- `Resultado` suma `agregarElegibilidad(regla, codigo)`; `agregar(...)` sigue
  creando motivos de PUNTAJE.
- Así se separan las reglas excluyentes de las de puntaje por el tipo de
  motivo, como pide la convención del proyecto.

| `@regla` | Tipo de motivo | Reglas DRL |
| --- | --- | --- |
| `edad` | ELEGIBILIDAD | `edad-elegible`, `edad-no-elegible` |
| `urgencia`, `documentacion`, `destino`, `situacion-laboral`, `antiguedad`, `clearing`, `carga` | PUNTAJE | sin cambios (23 reglas) |

### D3. Reglas DRL de edad

Dos reglas mutuamente excluyentes con `@regla("edad")`, que llaman a una
función nueva `emitirElegibilidad(drools, $r, codigo)`:

```
rule "edad-elegible"     when Solicitud( edadAlMenos(umbrales.edadMinima()) )
rule "edad-no-elegible"  when Solicitud( !edadAlMenos(umbrales.edadMinima()) )
```

`Solicitud.edadAlMenos(BigDecimal)` sigue el patrón de `antiguedadAlMenos`. El
DRL sigue siendo ASCII y sin números de negocio.

### D4. Precedencia en `CalculoEvaluacion`

- Nueva lista `REGLAS_EXCLUYENTES = List.of("edad")`. El orden de los motivos
  es excluyentes primero y después `ORDEN_REGLAS`, y la verificación de "un
  motivo por regla" cubre las dos listas (8 reglas).
- La suma considera solo los motivos de PUNTAJE.
- Si algún motivo de ELEGIBILIDAD es `no-elegible`: score 0, prioridad BAJA
  (sin consultar los umbrales de prioridad) y se agrega al final el motivo de
  ajuste. No se evalúa el recorte.
- Si no: el recorte y la prioridad de siempre.

Es el único lugar donde se decide la precedencia; ningún motivo de recorte y
de ajuste pueden aparecer juntos.

### D5. Textos

`TextosMotivos` suma:
- `edad/elegible` → "Edad de {edadMinima} años o más: elegible"
- `edad/no-elegible` → "No elegible: menor de {edadMinima} años"
- `ajustePorInelegibilidad(suma)` → "Score ajustado a 0 por inelegibilidad (suma {suma})", nuevo método de `RedaccionMotivos`.

Los motivos de ELEGIBILIDAD no llevan el sufijo ": puntos". La edad se escribe
con `stripTrailingZeros().toPlainString()`, como los demás umbrales.

### D6. Rango de `edad` y versión de reglas

- `ValidadorSolicitud`: `entero("edad", 1, 100)`.
- `VersionReglas.REVISION_LOGICA` pasa de 1 a 2 (cambian los textos y el
  cálculo). El test de cierre agrega `edad-elegible@edad` y
  `edad-no-elegible@edad` y pasa a la revisión 2.

## Risks / Trade-offs

- [Muchos tests del change 1 cuentan 7 motivos o prueban edad 13 y 14] → Las
  tareas los actualizan explícitamente y la cobertura de escenarios se vuelve a
  verificar con el script de `@DisplayName`.
- [Con la edad mínima en 1 el texto diría "Edad de 1 años o más"] → Es una
  configuración sin sentido de negocio (nadie sería no elegible); se acepta el
  plural fijo. Se documenta en el comentario de la propiedad.
- [El código `no-elegible` es un string compartido entre DRL y Java] → Es una
  constante en `Motivo`, y el test "exactamente un motivo por regla" y las
  tablas de la regla de edad detectan un desalineamiento.
- [`version_reglas` cambia para todos los leads] → Esperado: es justamente lo
  que permite saber con qué reglas se evaluó cada lead.

## Migration Plan

Sin cambios en n8n: el rango de `edad` se amplía y `motivos` se guarda unido
en una celda. Después del merge, reiniciar el servicio para que tome la nueva
configuración y las reglas.
