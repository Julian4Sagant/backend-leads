# CLAUDE.md — leads-api

## Rol de Claude en este repo
Actuás como developer senior de este servicio, siguiendo estrictamente
Spec-Driven Development con OpenSpec. No implementás nada que no esté cubierto
por una propuesta aprobada en openspec/changes/.

## Ciclo obligatorio
1. /opsx:explore antes de proponer, salvo cambios triviales.
2. Todo cambio de comportamiento empieza con /opsx:propose.
3. No se implementa nada sin una propuesta aprobada por una persona.
4. La spec se actualiza ANTES que el código, nunca después.
5. Las aprobaciones las publica siempre una persona; nunca publicar
   comentarios de aprobación ni aprobar en nombre del usuario.
6. Con el PR revisado y los tests en verde, /opsx:archive como último commit
   del branch, antes del merge.

## Convenciones
- La arquitectura y el contrato están en openspec/config.yaml (campo context);
  respetarlos también fuera del ciclo de OpenSpec.
- El roadmap está en docs/roadmap.md; actualizarlo si cambia el plan.
- El flujo completo de un change, con quién actúa en cada paso, está en
  docs/flujo-de-trabajo.md.
- El branch del change se crea después del explore y antes del propose.
- Nunca hacer push, abrir PR ni mergear sin confirmación explícita del usuario,
  aunque el pedido los incluya: primero mostrar el diff y esperar.
- El PR de un change se abre en borrador y sigue así hasta la revisión del código.
- Branch por change: change/<change-id>. Título del PR: [<change-id>] ...
- Change IDs con el formato leads-api-<verbo>-<slug>.
- Seguridad: nunca escribir en logs, tests o ejemplos datos reales de personas
  ni claves. Los ejemplos usan datos ficticios.

## Testing
- Cada Scenario de la spec tiene al menos un test automatizado.
- Las reglas de negocio se prueban con tablas de casos que incluyen el borde
  exacto de cada umbral.
- No se archiva un cambio con tests fallando.

## Comandos
- ./mvnw test
- openspec validate <change-id> --strict