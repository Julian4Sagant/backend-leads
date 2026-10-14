# Integración con n8n y ngrok

Guía de operación: cómo se levanta leads-api en una PC, cómo lo alcanza n8n
Cloud a través de ngrok y cómo está armado el flujo que guarda el resultado en
la planilla. Verificada de punta a punta el 2026-10-13, con los tres caminos
del flujo (lead evaluado, dato inválido y servicio caído).

Los valores de ejemplo están entre `<...>`. En este documento no van la clave
de API, el token de ngrok ni el dominio real de ngrok.

## Cómo encaja todo

```
  +-----------+     +------------------+     +---------+     +-----------+
  | Chat n8n  | --> | Flujo n8n Cloud  | --> |  ngrok  | --> | leads-api |
  | (persona) |     | (12 preguntas)   |     | (túnel) |     | (tu PC,   |
  +-----------+     +------------------+     +---------+     |  :8080)   |
                         |       ^                           +-----------+
                         v       |
                    +------------------+
                    | Google Sheets    |
                    | Leads + criterio |
                    +------------------+
```

1. El chat hace las 12 preguntas. Cada respuesta se valida contra la pestaña
   `criterio` de la planilla antes de guardarla en `Leads`.
2. Con el lead completo, el flujo relee la fila y la manda a `POST /evaluar`
   por la URL de ngrok, con el header `X-API-Key`.
3. El resultado (o el error) se guarda en la misma fila, buscándola por
   `session_id`. La persona ve siempre el mismo mensaje final, sin el
   resultado.

leads-api no guarda nada: cada pedido se evalúa solo. Todo el estado vive en
la planilla y en la memoria del chat de n8n.

## 1. Clave de API (una sola vez)

La clave tiene que tener al menos 32 caracteres ASCII imprimibles y sin
espacios. Se genera una vez y se guarda como variable de usuario de Windows,
así la ven todas las terminales nuevas:

```powershell
$bytes = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes); $env:LEADS_API_KEY = [Convert]::ToBase64String($bytes)
```

```powershell
[Environment]::SetEnvironmentVariable("LEADS_API_KEY", $env:LEADS_API_KEY, "User")
```

- Guardá la clave también en tu gestor de contraseñas: es la misma que va en
  la credencial de n8n.
- **Las terminales leen las variables de usuario solo al abrirse.** Una
  terminal abierta antes de guardar la clave no la ve, y el servicio no
  arranca (`BUILD FAILURE`). Cerrala y abrí otra.
- Para comprobar que una terminal la tiene sin mostrarla:
  `$env:LEADS_API_KEY.Length` (con la clave generada así, da 44).
- Nunca pegues la clave en un chat, un ticket o una captura.

## 2. Levantar el servicio y el túnel

Hacen falta dos terminales nuevas, abiertas en `C:\DEV\backend-leads`.

**Terminal 1, el servicio** (puerto 8080). Esperá `Started BackendLeadsApplication`:

```powershell
.\mvnw.cmd spring-boot:run
```

Si no arranca, el motivo está más arriba del `BUILD FAILURE`: buscá
`APPLICATION FAILED TO START` o `Caused by`. La aplicación tampoco arranca si
la configuración de reglas de `application.yml` es incoherente, y lo dice.

**Terminal 2, ngrok**, con tu dominio fijo de ngrok. Esperá `Session Status online`:

```powershell
ngrok http --domain=<tu-dominio>.ngrok-free.dev 8080
```

- Hace falta ngrok 3.x actualizado. Si el agente es viejo, ngrok lo rechaza o
  no reconoce las opciones: `ngrok update`.
- El inspector de ngrok (http://127.0.0.1:4040) muestra los headers completos,
  **incluida la `X-API-Key`**. No compartas capturas de esa página.

**Prueba rápida** desde una tercera terminal, con datos ficticios:

```powershell
$cuerpo = '{"session_id":"prueba-ngrok-1","edad":32,"motivo":"auto","monto":150000,"plazo_meses":24,"urgencia":"esta_semana","situacion_laboral":"dependiente","antiguedad_meses":36,"ingreso_mensual":60000,"cuota_deudas":5000,"clearing":"no","documentacion":"si"}'; Invoke-RestMethod -Method Post -Uri https://<tu-dominio>.ngrok-free.dev/evaluar -Headers @{ "X-API-Key" = $env:LEADS_API_KEY } -ContentType "application/json" -Body $cuerpo | ConvertTo-Json
```

Tiene que responder `prioridad` ALTA, `score` 85, 8 motivos (el primero,
"Edad de 18 años o más: elegible") y un `version_reglas`. En ngrok aparece
`POST /evaluar 200` y en el servicio una línea
`evaluacion session_id=prueba-ngrok-1 prioridad=ALTA score=85 ...`.

**Al terminar, cortá ngrok** (Ctrl+C). El servicio rechaza pedidos de más de
16 KB y exige la clave, pero corre en tu PC: no conviene dejarlo expuesto
cuando nadie lo usa.

**Después de cada merge que cambie el servicio**, reinicialo (Ctrl+C y
`.\mvnw.cmd spring-boot:run` de nuevo) para que tome las reglas nuevas.

## 3. La planilla

Documento `calificador_prestamos`:

- **`Leads`:** una fila por conversación, identificada por `session_id`, con
  los 12 campos del contrato y las columnas de resultado `prioridad`, `score`,
  `motivos` y `version_reglas`. Update Row no crea columnas: tienen que
  existir.
- **`criterio`:** el criterio con el que el chat valida cada respuesta. Si una
  respuesta no lo cumple, el chat repite la pregunta y, al tercer intento,
  termina la conversación sin llamar al servicio.

**El criterio tiene que coincidir con lo que acepta el servicio.** Si es más
estricto, el lead nunca llega a evaluarse; si es más permisivo, el servicio
responde 400 y el lead queda en `ERROR`. Hoy, por ejemplo, la edad se valida
como un entero de 1 a 100: un menor de 18 no es un error, el servicio lo
evalúa como no elegible. Cuando un change modifique rangos o valores
permitidos del contrato, hay que actualizar esta pestaña.

## 4. El flujo de n8n

Después de que el chat marca el lead como completo (`Marcar completo`):

```
Marcar completo --> Releer lead --> Evaluar lead
                                      |
                 +--------------------+---------------------+
                 | Success                                  | Error
                 v                                          v
            Evaluado?                              Guardar sin respuesta
          true |      | false                               |
               v      v                                     |
  Guardar evaluación  Guardar error evaluación              |
               |      |                                     |
               +------+-------------------------------------+
                                  |
                                  v
                              Despedida
```

### Credencial

Credentials, New, **Header Auth**: Name `X-API-Key`, Value la clave (la ves
con `$env:LEADS_API_KEY` en una terminal nueva).

### Releer lead (Google Sheets, Get row(s))

Hoja `Leads`, filtro con **Column** `session_id` (modo Fixed, elegido de la
lista) y **Value** `{{ $('Marcar completo').item.json.session_id }}` (modo
Expression). Si la expresión queda en Column, n8n busca una columna con el
nombre del `session_id` y falla.

Tiene que devolver los 12 campos. Los números pueden venir como `150000` o
`"150000"`, pero no con separador de miles ni coma decimal (`"150.000"` da 400).

### Evaluar lead (HTTP Request)

| Campo | Valor |
| --- | --- |
| Method | `POST` |
| URL | `https://<tu-dominio>.ngrok-free.dev/evaluar` |
| Authentication | Generic Credential Type, Header Auth, la credencial `X-API-Key` |
| Send Body | Activado, Body Content Type **JSON**, Specify Body **Using JSON** |
| JSON | La expresión de abajo, en modo Expression |

```
{{ JSON.stringify({
  session_id: $json.session_id,
  edad: $json.edad,
  motivo: $json.motivo,
  monto: $json.monto,
  plazo_meses: $json.plazo_meses,
  urgencia: $json.urgencia,
  situacion_laboral: $json.situacion_laboral,
  antiguedad_meses: $json.antiguedad_meses,
  ingreso_mensual: $json.ingreso_mensual,
  cuota_deudas: $json.cuota_deudas,
  clearing: $json.clearing,
  documentacion: $json.documentacion
}) }}
```

- **Sin `Number()`** sobre los campos: convertiría un campo vacío en `0` y el
  servicio lo evaluaría en vez de responder `requerido`.
- Options, Response: **Include Response Headers and Status** activado,
  **Never Error** activado y **Response Format JSON**. Con Autodetect, los
  errores (que llegan como `application/problem+json`) pueden quedar como
  texto y se pierde el `type`.
- Settings: **Retry On Fail** con 3 intentos y **On Error: Continue (using
  error output)**.

### Evaluado? (IF)

Condición `{{ $json.statusCode }}`, Number, is equal to `200`.

### Los tres Update Row

Los tres sobre la hoja `Leads`, con **Column to match on** `session_id` y el
valor de match `{{ $('Releer lead').item.json.session_id }}`. Con
`$json.session_id` el match queda vacío: en estas ramas `$json` es la
respuesta del servicio, no la fila.

| Nodo | Sale de | `prioridad` | `score` | `motivos` | `version_reglas` |
| --- | --- | --- | --- | --- | --- |
| Guardar evaluación | IF, true | `{{ $json.body.prioridad }}` | `{{ $json.body.score }}` | `{{ $json.body.motivos.join(" \| ") }}` | `{{ $json.body.version_reglas }}` |
| Guardar error evaluación | IF, false | `ERROR` | vacío | ver abajo | vacío |
| Guardar sin respuesta | Evaluar lead, Error | `ERROR` | vacío | `sin respuesta del servicio` | vacío |

`motivos` de **Guardar error evaluación** registra el código, el `type` y los
campos que fallaron, sin sus valores:

```
{{ $json.statusCode }} {{ $json.body.type }} {{ ($json.body.errores || []).map(e => e.campo + ':' + e.codigo).join(', ') }}
```

No guardes el `mensaje` de cada error: no es parte del contrato.

### Despedida

Recibe las tres ramas. Si usa datos del lead, tomalos de
`{{ $('Marcar completo').item.json... }}` y no de `$json`.

## 5. Cómo leer la planilla

| Lo que ves | Qué significa | Qué hacer |
| --- | --- | --- |
| Prioridad, score, motivos y `version_reglas` | Lead evaluado | Nada |
| Score 0, BAJA, primer motivo "No elegible: menor de 18 años" | Lead evaluado, no elegible por edad | Nada: es el resultado buscado |
| `ERROR` y `401 ...no-autorizado` | La clave de la credencial no coincide con la del servicio | Revisar la credencial |
| `ERROR` y `415 ...` | El body no sale como JSON | Revisar Evaluar lead |
| `ERROR` y `400 ...validacion campo:codigo` | Un dato del lead mal formado | Revisar el criterio o la normalización de ese campo |
| `ERROR` y `413 ...cuerpo-demasiado-grande` | El pedido pasó los 16 KB (los de n8n pesan unos 300 bytes) | Revisar qué armó Evaluar lead: no debería pasar |
| `ERROR` y `sin respuesta del servicio` | El servicio, ngrok o la red no respondieron (después de 3 intentos) | Levantar el servicio y ngrok |

Los leads en `ERROR` hoy no se reintentan solos (ver el roadmap, "Pendientes
fuera del plan").

## 6. Cómo probar

Con el servicio y ngrok corriendo:

1. **Empezá siempre con una sesión nueva del chat.** El chat guarda el
   `session_id` en el navegador: si reusás la sesión, la memoria del chat y la
   fila existente hacen que el flujo responda como si la solicitud ya
   estuviera hecha. En el panel de chat del editor, reiniciá la sesión; en la
   URL pública, usá una ventana de incógnito.
2. **Lead correcto:** conversación completa con datos ficticios. La fila queda
   con el resultado.
3. **Menor de edad:** respondé una edad de 16. El chat no tiene que repetir la
   pregunta, y la fila queda con score 0, BAJA, "No elegible: menor de 18
   años" y al final "Score ajustado a 0 por inelegibilidad (suma X)".
4. **Dato inválido:** en la fila de una prueba, cambiá `ingreso_mensual` a
   `abc`, abrí esa ejecución en el editor y ejecutá Releer lead, Evaluar lead,
   el IF y Guardar error evaluación. Tiene que quedar `ERROR` con
   `400 urn:leads-api:error:validacion ingreso_mensual:formato-invalido`.
   Después volvé el valor original.
5. **Servicio caído:** detené el servicio con ngrok corriendo y repetí el
   paso anterior. ngrok responde con una página HTML que n8n no puede leer
   como JSON: sale por la rama Error y queda `sin respuesta del servicio`.

En todos los casos la persona del chat ve la misma Despedida.
