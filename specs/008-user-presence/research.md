# Research: Estado de presencia / ocupación del usuario

**Feature**: `008-user-presence` | **Fase**: 0 (Outline & Research)

No quedaron marcadores `NEEDS CLARIFICATION` en el Technical Context. Esta investigación documenta
las decisiones de diseño tomadas sobre la base del código existente.

---

## Decisión 1 — Ruta y forma del endpoint

- **Decisión**: `GET /api/me/presence`, protegido con JWT, sin parámetros de ruta ni query. La
  identidad sale del token (`jwt.getSubject()`), como en el resto de controllers.
- **Rationale**: FR-001 exige operar sobre el usuario autenticado **sin** recibir el id de usuario
  como parámetro (no exponer estado de terceros). El prefijo `/api/me` comunica claramente
  "recurso del usuario actual" y deja espacio para futuras vistas del propio usuario sin colisionar
  con `/api/matches`, `/api/leagues`, `/api/cups`.
- **Alternativas consideradas**:
    - `GET /api/presence`: válido, pero `/api/me/presence` es más explícito sobre el sujeto.
    - `GET /api/users/{id}/presence`: rechazado — violaría FR-001 al aceptar un id externo.

## Decisión 2 — Capa donde vive la agregación

- **Decisión**: La agregación se hace en un **query handler de aplicación**
  (`GetUserPresenceQueryHandler`), orquestando lecturas sobre los puertos de salida de cada dominio.
  No se crea un agregado de dominio ni un nuevo bounded context.
- **Rationale**: Principio V (YAGNI) y II (Dominio Puro). La presencia no tiene invariantes ni
  transiciones de estado propias: es una **vista de lectura** que combina datos ya existentes en
  cuatro dominios. El patrón coincide con las queries existentes (`GetRematchSessionQueryHandler`,
  `GetMatchStateQueryHandler`), que también orquestan repositorios y arman un DTO.
- **Alternativas consideradas**:
    - Un agregado `Presence` en `domain`: rechazado, no posee estado persistente ni reglas; sería
      una abstracción prematura.
    - Resolver en el controller: rechazado, viola la regla de que la orquestación de casos de uso
      vive en `application`.

## Decisión 3 — Cómo obtener la partida activa del usuario

- **Decisión**: Agregar `Optional<Match> findUnfinishedByPlayer(PlayerId)` a `MatchQueryRepository`,
  con la misma semántica de "no finalizada" que el ya existente `hasUnfinishedMatch`
  (`status NOT IN ('FINISHED','CANCELLED')`). La implementación JPA reutiliza el patrón de
  `SpringDataMatchRepository`.
- **Rationale**: El repositorio ya tiene `hasUnfinishedMatch(PlayerId)` (booleano), pero la
  presencia necesita el **identificador y estado** de la partida (FR-003), no solo saber si existe.
  Es el único método de consulta faltante; el resto de dominios ya exponen lo necesario.
- **Invariante de unicidad**: El sistema impide que un jugador esté en más de una partida no
  finalizada simultáneamente (se valida al unirse/crear). Por lo tanto `findUnfinishedByPlayer`
  devuelve a lo sumo un resultado. Si hubiera más de una por una condición de carrera histórica,
  la consulta debe ser determinista (ordenar por `lastActivityAt DESC`) y devolver la más reciente.
- **Alternativas consideradas**:
    - Reusar `hasUnfinishedMatch` + `findById`: imposible, no hay forma de obtener el id desde el
      booleano.
    - Devolver una proyección liviana (solo id+status) en vez de `Match`: válido, pero romper la
      convención (los demás `findXByPlayer` devuelven el agregado) agrega complejidad sin beneficio
      a esta escala. Se devuelve `Optional<Match>` y el handler extrae `getId()` y `getStatus()`.

## Decisión 4 — Liga y copa: estado + partida actual del torneo

- **Decisión**: Reutilizar los métodos existentes `findInProgressByPlayer(PlayerId)` y
  `findWaitingByPlayer(PlayerId)` de `LeagueQueryRepository` y `CupQueryRepository`. Para el
  `currentMatchId` del torneo en progreso (FR-004, FR-005), se reutiliza la **partida no finalizada
  ya resuelta en la Decisión 3**: si el usuario está en un torneo `IN_PROGRESS` y tiene una partida
  no finalizada, esa partida es la del torneo (por la invariante de unicidad de la Decisión 3).
- **Rationale**: Evita consultas adicionales y mantiene coherencia con el "Edge Case: ocupación
  simultánea coherente" del spec: una partida que pertenece a un torneo se refleja en **ambos**
  dominios (torneo + partida actual). El `currentMatchId` apunta a la misma partida que el campo
  `match` de nivel superior.
- **Orden de resolución**: primero `findInProgressByPlayer`; si está vacío, `findWaitingByPlayer`.
  Un torneo en espera no tiene partida actual → `currentMatchId` nulo (FR-004/FR-005 lo piden solo
  "cuando esté en progreso").
- **Alternativas consideradas**:
    - Consultar `LeagueQueryRepository.findByMatchId(matchId)` para confirmar el vínculo: agrega un
      roundtrip innecesario dado el invariante de unicidad. Se documenta como assumption
      verificable.

## Decisión 5 — Revancha

- **Decisión**: Reutilizar `RematchSessionRepository.findOpenByPlayer(PlayerId)`. De existir, se
  expone el id de la sesión y `originMatchId` (FR-006).
- **Rationale**: Método ya existente con la semántica exacta requerida ("sesión de revancha
  abierta"). Coincide con `rematch_eligibility_rules` y el diseño del bounded context rematch.

## Decisión 6 — Indicador general de ocupación y representación de vacío

- **Decisión**: `busy = (match != null) || (league != null) || (cup != null) || (rematch != null)`.
  Los dominios sin ocupación se serializan como `null` explícito en el JSON (no se omiten).
- **Rationale**: FR-007 (busy verdadero sii al menos un dominio tiene contenido) y FR-008
  (representar el vacío de forma inequívoca). `null` explícito por dominio es inequívoco para el FE
  y más simple que objetos "vacíos" con flags.
- **Alternativas consideradas**:
    - Omitir las claves nulas: rechazado, FR-008 pide ausencia inequívoca, no omisión silenciosa.

## Decisión 7 — Solo lectura / sin efectos secundarios

- **Decisión**: El handler usa exclusivamente métodos de los `*QueryRepository` / `find*` de
  lectura. No invoca comandos, no publica eventos, no toca `lastActivityAt` ni temporizadores.
- **Rationale**: FR-009 y SC-005. Se verificará con un test que consulta repetidamente y comprueba
  que el estado de los recursos y sus marcas de actividad no cambian.

## Decisión 8 — Quick match fuera de alcance

- **Decisión**: No se consulta `QuickMatchQueuePort`. El DTO de presencia no incluye quick match.
- **Rationale**: FR-010 + Assumptions del spec: la cola de quick match no sobrevive a la
  desconexión, por lo que nunca es un estado reconectable.

## Documentación a actualizar

- **`README.md`**: agrega un recurso REST nuevo (`GET /api/me/presence`) → listar en la sección de
  endpoints/capacidades del sistema.
- **`docs/CONTRATOS_API.md`**: endpoint REST nuevo con su shape de respuesta → agregar la sección
  del contrato de presencia (request, respuesta de usuario ocupado y de usuario libre, códigos 200
  y 401). No agrega ni quita eventTypes WebSocket ni enums nuevos (reutiliza `MatchStatus`,
  `LeagueStatus`, `CupStatus` y el estado de la sesión de revancha ya documentados).
