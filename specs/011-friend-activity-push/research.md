# Investigacion: Actividad en vivo de amigos

**Feature**: `011-friend-activity-push` | **Fase**: 0 (Outline & Research)

## 1. Canal para actividad de amigos

**Decision**: Reutilizar la cola personal social `/user/queue/social` para publicar actividad de
amigos.

**Rationale**: La actividad de amigos es informacion social del usuario autenticado. El proyecto ya
tiene una cola social para amistades e invitaciones, con autenticacion y routing por usuario. Crear
un destino por amigo o una cola nueva agregaria superficie operativa sin cambiar la semantica.

**Alternativas consideradas**:

- Nuevo destino `/user/queue/friend-activity`: rechazado por duplicar una cola social ya existente.
- Un destino por amigo: rechazado por escalar mal y complicar suscripciones.
- Reutilizar `/user/queue/match-spectate`: rechazado porque ese flujo registra espectadores y envia
  estado de match, no presencia social.

## 2. Consistencia entre GET inicial y deltas

**Decision**: Mantener `GET /api/social/friends` como bootstrap y enviar un evento
`FRIEND_ACTIVITY_STATE` al suscribirse a la cola social.

**Rationale**: Si el cliente hace GET y luego se suscribe, puede perder un cambio ocurrido entre
ambas operaciones. Un snapshot al suscribirse permite que la vista converja al estado vigente sin
obligar al cliente a adivinar si perdio deltas.

**Alternativas consideradas**:

- Exigir que el cliente se suscriba antes del GET: rechazado porque no siempre coincide con el flujo
  actual del cliente y no protege reconexiones parciales.
- Solo publicar deltas: rechazado por la carrera GET/deltas.
- Polling periodico: rechazado por innecesario y menos eficiente que un snapshot de reconexion.

## 3. Modelo de payload

**Decision**: Representar cada amigo con `friendUsername` y `spectatableMatch` nullable. La
referencia
de match mantiene solo `id` y `status`.

**Rationale**: Es el mismo concepto que la lista de amigos enriquecida por la feature de spectate.
El cliente necesita saber si puede mostrar "Espectar" y con que `matchId` iniciar el flujo
existente,
pero no debe recibir cartas, acciones ni detalle de ronda.

**Alternativas consideradas**:

- Enviar un booleano `isPlaying`: rechazado porque obliga a otro lookup para obtener el match
  espectable.
- Enviar resumen completo de partida: rechazado por privacidad y duplicacion del flujo de spectate.
- Enviar datos de ambos jugadores: rechazado para minimizar exposicion; el contexto social ya indica
  que el cambio corresponde al amigo.

## 4. Origen de los cambios

**Decision**: Traducir eventos de match que cambian disponibilidad espectable: inicio de partida y
finalizacion/cancelacion/abandono/forfeit.

**Rationale**: La disponibilidad social cambia cuando el match entra o sale de `IN_PROGRESS`. Los
eventos de match existentes tienen `matchId`, `playerOne` y `playerTwo`, suficientes para calcular
amigos destinatarios y payload minimo.

**Alternativas consideradas**:

- Recalcular actividad en cada evento de match: rechazado por ruido y eventos duplicados.
- Persistir tabla de actividad: rechazado porque el estado se deriva de matches y amistades.
- Publicar desde controllers/comandos concretos: rechazado porque dispersa reglas y omite flujos
  como revancha, liga o copa.

## 5. Destinatarios y privacidad

**Decision**: Enviar actividad solo a amigos aceptados vigentes de los jugadores involucrados, nunca
a solicitudes pendientes ni amistades removidas.

**Rationale**: La feature se basa en amistad confirmada como limite de visibilidad. Ademas, si una
amistad se elimina, el estado debe desaparecer y no deben salir mas novedades para esa relacion.

**Alternativas consideradas**:

- Enviar a todos los usuarios conectados: rechazado por privacidad.
- Enviar a participantes de liga/copa: rechazado porque esta feature es social; liga/copa tiene
  flujos propios.
- Mantener actividad visible hasta refresco manual: rechazado por inconsistencias y privacidad.

## 6. Relacion con spectate

**Decision**: La actividad de amigos descubre disponibilidad, pero no registra espectadores. El alta
de spectate sigue en el flujo existente con `matchId`.

**Rationale**: Separar descubrimiento de ingreso mantiene reglas centralizadas: elegibilidad,
snapshot de espectador, errores y cleanup siguen en spectate. La actividad social solo actualiza la
lista de amigos.

**Alternativas consideradas**:

- Registrar automaticamente spectate al recibir actividad: rechazado porque mirar una partida es una
  accion explicita del usuario.
- Crear alta REST para amigos: rechazado porque duplica el contrato existente.

## 7. Reconexion

**Decision**: Tras una nueva suscripcion social, el backend vuelve a enviar el snapshot
`FRIEND_ACTIVITY_STATE`.

**Rationale**: El contrato actual de WebSocket ya asume re-suscripcion en reconexion para spectate.
Aplicar el mismo criterio a social permite reconstruir estado sin persistir sesiones de presencia.

**Alternativas consideradas**:

- Persistir ultimos eventos por usuario: rechazado por YAGNI.
- Requerir que el cliente siempre refresque manualmente: rechazado porque empeora la UX y no cumple
  el objetivo en vivo.
