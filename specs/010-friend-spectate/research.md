# Investigación: Espectar partidas de amigos

**Feature**: `010-friend-spectate` | **Fase**: 0 (Outline & Research)

## 1. Mecanismo de alta de espectador

**Decisión**: Mantener el flujo actual WebSocket-first. Para empezar a mirar, el cliente se suscribe
a `/user/queue/match-spectate` enviando `matchId`; si el alta es válida, recibe `SPECTATE_STATE`.
`GET /api/matches/{matchId}/spectate` sigue siendo solo snapshot para un espectador ya registrado.

**Rationale**: El proyecto ya centraliza registro, conteo, reconexión, errores y cleanup de
spectatorship en ese flujo. Agregar un endpoint REST de alta duplicaría reglas y rompería la
arquitectura existente.

**Alternativas consideradas**:

- Nuevo endpoint `POST /api/matches/{matchId}/spectate`: rechazado por duplicar el alta actual.
- Flujo social dedicado: rechazado porque generaría dos caminos de spectatorship.

## 2. Elegibilidad por amistad

**Decisión**: Extender `SpectatingEligibilityPolicy` para aceptar dos motivos válidos: pertenecer a
la misma liga/copa del match o tener amistad aceptada con al menos uno de los jugadores del match.
Se agrega un puerto puro en `domain.ports` para resolver amistad aceptada.

**Rationale**: La política actual ya valida estado del match, no spectear partida propia y una única
sesión activa de spectate. La amistad es una condición de elegibilidad adicional, no un caso de uso
nuevo.

**Alternativas consideradas**:

- Validar amistad en `SpectateMatchCommandHandler`: rechazado porque fragmenta reglas de acceso.
- Importar `FriendshipQueryRepository` desde dominio: rechazado porque viola bounded contexts y
  dominio puro.
- Reemplazar `CompetitionMembershipResolver`: rechazado porque liga/copa deben seguir funcionando.

## 3. Adapter social hacia el dominio principal

**Decisión**: Implementar un adapter de aplicación social, similar a
`SocialFriendshipParticipantsSupport`, que use `FriendshipQueryRepository.existsAcceptedByPlayers`
para responder al puerto de elegibilidad de spectate.

**Rationale**: Social ya posee el repositorio de amistades y sus invariantes. El dominio de spectate
solo necesita saber si dos jugadores son amigos confirmados, sin conocer cómo se persiste ni modela
la amistad.

**Alternativas consideradas**:

- Query JPA directa desde `SpectatorConfiguration`: rechazada por saltarse el puerto.
- Servicio en infrastructure principal: rechazado porque la lógica pertenece al bounded context
  social.

## 4. Descubrimiento de partidas espectables desde amigos

**Decisión**: Enriquecer `GET /api/social/friends` para devolver `spectatableMatch` nullable por
amigo. El campo aparece con `id` y `status` solo si el amigo tiene un match `IN_PROGRESS` que el
usuario puede intentar espectar.

**Rationale**: El cliente necesita `matchId` para iniciar el flujo existente. La lista de amigos es
la superficie natural para mostrar la acción "Espectar" sin crear un endpoint separado de búsqueda.

**Alternativas consideradas**:

- Agregar endpoint `GET /api/social/friends/{username}/spectatable-match`: rechazado por aumentar
  roundtrips y complejidad.
- Usar solo presencia del usuario autenticado: insuficiente, porque describe la ocupación propia,
  no la de sus amigos.
- Exponer todas las partidas de amigos: rechazado por alcance; v1 muestra como máximo una partida
  activa por amigo.

## 5. Información visible para espectadores

**Decisión**: Reutilizar `SpectatorMatchStateDTOAssembler` y los eventos de espectador existentes,
que ya excluyen cartas privadas y eventos por asiento como `PLAYER_HAND_UPDATED` y
`AVAILABLE_ACTIONS_UPDATED`.

**Rationale**: La integridad competitiva depende de no filtrar estado privado. La vista de
espectador existente ya modela esa separación.

**Alternativas consideradas**:

- Usar la vista de jugador para amigos: rechazado por revelar información privada.
- Agregar una vista especial para amigos: rechazado porque no hay requerimiento de contenido
  distinto.

## 6. Opt-out de espectadores amigos

**Decisión**: No crear preferencia ni configuración de privacidad para deshabilitar que amigos
confirmados especten.

**Rationale**: Es una regla explícita de producto de esta feature. El límite de privacidad queda en
la amistad confirmada.

**Alternativas consideradas**:

- Opt-out por usuario o por match: rechazado por requerimiento explícito.
- Default activado con override: rechazado por el mismo motivo.

## 7. Persistencia de spectatorship

**Decisión**: Mantener `SpectatorshipRepository` en memoria y la limpieza actual por
`UNSUBSCRIBE`/`DISCONNECT`/match terminado.

**Rationale**: La feature no cambia el ciclo de vida de spectatorship. Persistir sesiones de
espectador introduce complejidad sin valor para reconexión, porque el contrato actual indica
re-suscripción.

**Alternativas consideradas**:

- Tabla `spectatorships`: rechazada por YAGNI.
- Reanudar automáticamente sin re-suscripción: rechazada por contrato actual.

## 8. Corte al eliminar amistad

**Decisión**: Agregar un handler de aplicación para `FRIENDSHIP_REMOVED` que busque spectatorships
activos de los dos participantes y corte los que ya no tengan elegibilidad válida para el match
observado. Si el espectador conserva elegibilidad por liga/copa, no se corta.

**Rationale**: La spec exige invalidar acceso cuando la relación de amistad deja de ser válida, pero
la feature también preserva los modos existentes de liga/copa. Por eso el criterio correcto es
reevaluar elegibilidad total, no cortar ciegamente todos los spectatorships entre esos dos usuarios.

**Alternativas consideradas**:

- Cortar siempre ante `FRIENDSHIP_REMOVED`: rechazado porque rompería espectadores que además son
  miembros de la misma liga/copa.
- Esperar al próximo reconnect: rechazado porque incumple la invalidación durante sesión.
- Agregar polling: rechazado por innecesario; ya existe el evento social.
