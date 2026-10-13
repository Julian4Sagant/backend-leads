# leads-api

Servicio que califica solicitudes de préstamo. Recibe en `POST /evaluar` una
solicitud ya normalizada por el chat de n8n (12 campos) y devuelve la
prioridad (ALTA, MEDIA o BAJA), un score de 0 a 100, los motivos en español y
la versión de las reglas con que se evaluó. No guarda nada: cada pedido se
evalúa solo.

Spring Boot 4.1 con Java 21 y las reglas en Drools. Se accede con el header
`X-API-Key`.

## Correr el servicio

Hace falta la variable de entorno `LEADS_API_KEY` (32 caracteres o más, ASCII
imprimible y sin espacios); sin ella el servicio no arranca.

```powershell
.\mvnw.cmd spring-boot:run
```

Tests:

```powershell
.\mvnw.cmd test
```

## Documentación

- [Integración con n8n y ngrok](docs/integracion-n8n.md): cómo levantar el
  servicio, exponerlo con ngrok, el flujo de n8n y cómo probarlo.
- [Roadmap](docs/roadmap.md): changes, decisiones y pendientes.
- [Notas técnicas](docs/notas-tecnicas-leads.md): decisiones técnicas y su
  justificación.
- [Flujo de trabajo](docs/flujo-de-trabajo.md): cómo se hace un change con
  OpenSpec y quién actúa en cada paso.
- Specs vigentes en [openspec/specs](openspec/specs): `evaluacion-leads` y
  `acceso-api`.
