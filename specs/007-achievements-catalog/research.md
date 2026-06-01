# Research: Catálogo de Logros

No quedaron marcadores `NEEDS CLARIFICATION` en la spec. Las decisiones de diseño se cerraron en la
conversación previa; se documentan acá para trazabilidad.

## Decisión 1 — Fuente de verdad del catálogo

**Decisión**: El catálogo se deriva directamente del enum de dominio `AchievementCode`
(`AchievementCode.values()`). No se crea una tabla ni una entidad de catálogo.

**Rationale**: El conjunto de logros ya está modelado como enum en el dominio y es la fuente que usa
`AchievementPolicy` para decidir desbloqueos. Derivar el endpoint del enum garantiza una única
fuente de verdad (FR-004): agregar un valor al enum lo hace aparecer en el catálogo sin pasos extra
ni migraciones. Persistir un catálogo aparte duplicaría la información y abriría desincronización.

**Alternativas consideradas**:
- *Tabla de catálogo en base de datos*: rechazada por YAGNI (Principio V) y porque duplica el enum.
- *Constante hardcodeada en infraestructura*: rechazada; volvería a partir la fuente de verdad.

## Decisión 2 — Forma de la respuesta

**Decisión**: `GET /api/achievements` devuelve `{ "achievements": [ { "achievementCode": "..." } ] }`
— una lista de objetos, cada uno con el campo `achievementCode`.

**Rationale**: El nombre de campo `achievementCode` coincide exactamente con el que ya usa el perfil
en `achievements[].achievementCode`. Así el frontend mergea ambos conjuntos por la misma clave sin
adaptaciones. Usar objetos (en vez de un array plano de strings) deja margen de evolución sin romper
el contrato si en el futuro se agregaran campos al catálogo.

**Alternativas consideradas**:
- *Array plano de strings* `["CODE1", ...]`: más mínimo, pero rompe consistencia de naming con el
  perfil y obligaría a un cambio de contrato si después se agrega cualquier campo.

## Decisión 3 — Sin título/descripción ni logros ocultos

**Decisión**: El catálogo expone solo códigos. Los textos de presentación los resuelve el frontend a
partir del código (FR-006). No existe el concepto de logro oculto (FR-007).

**Rationale**: Acordado con el usuario. El título/descripción es preocupación de presentación e
i18n: vive en el frontend, que ya lo hace hoy. Mantener el backend dueño de *qué existe* y el
frontend dueño de *cómo se muestra* respeta la separación de capas y evita acoplar copy al backend.

**Alternativas consideradas**:
- *Metadatos en el enum + endpoint con título/descripción*: rechazada; mueve i18n y copy al backend
  y obliga a deploy de API para cambiar textos.
- *Flag `hidden` con descripción enmascarada*: rechazada explícitamente por el usuario.

## Decisión 4 — Ubicación del endpoint

**Decisión**: Nuevo `AchievementController` en `com.villo.truco.profile.infrastructure.http`, ruta
`GET /api/achievements`. El controller depende del puerto de entrada
`GetAchievementCatalogUseCase`.

**Rationale**: El catálogo es global (no depende de `{username}`), por lo que no encaja como
sub-recurso de `/api/profile/{username}`. Vive en el bounded context `profile` porque ahí está el
dominio de logros. El paso por puerto de entrada cumple el Principio I (reforzado por ArchUnit).

**Alternativas consideradas**:
- *Colgarlo de `ProfileController`*: rechazada; mezcla un recurso global con uno por jugador.

## Decisión 5 — Protección contra desincronización (FR-008)

**Decisión**: Test de contrato `AchievementCatalogContractTest` que compara el conjunto de
`AchievementCode.values()` contra los códigos listados en la sección §8.3 de
`docs/CONTRATOS_API.md`. Falla si hay códigos en uno y no en el otro.

**Rationale**: El frontend mapea código → texto; si el backend agrega un código sin documentarlo, el
frontend se entera tarde. Un test barato que parsea la lista canónica de §8.3 detecta el desfasaje
en build (Principio III). La sección §8.3 ya existe y enumera los códigos uno por línea, formato
trivial de parsear.

**Alternativas consideradas**:
- *Confiar en revisión manual / `CLAUDE.md`*: rechazada; no falla el build y depende de memoria
  humana.
- *Generar la doc desde el enum*: sobre-ingeniería para 10 valores; el test de igualdad alcanza.

## Confirmaciones del código existente

- `AchievementCode` ([src/main/java/com/villo/truco/profile/domain/model/AchievementCode.java](../../src/main/java/com/villo/truco/profile/domain/model/AchievementCode.java))
  hoy es un enum sin metadatos: encaja como fuente del catálogo sin cambios.
- `ProfileController` expone `GET /api/profile/{username}` y devuelve solo desbloqueados; no se
  toca.
- `ProfileUseCaseConfiguration` ya cablea casos de uso de `profile` vía `transactionalPipeline`;
  el nuevo bean sigue el mismo patrón (aunque el catálogo no necesita transacción, se mantiene la
  consistencia del wiring del módulo).
- `docs/CONTRATOS_API.md` §8.3 enumera los 10 códigos actuales y coincide con el enum. El ejemplo
  del perfil (~línea 1646) usa un código obsoleto `WIN_RETRUCO_FROM_0_0_TO_3` que conviene corregir
  de paso.
