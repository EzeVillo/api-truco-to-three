# Data Model: Actividad en vivo de amigos

## Entidades y modelos

### FriendActivity

Modelo de lectura que representa el estado visible de un amigo confirmado desde la perspectiva del
jugador autenticado.

**Campos**:

- `friendUsername`: username publico del amigo.
- `spectatableMatch`: nullable. Referencia minima a la partida espectable actual.

**Reglas**:

- Solo existe para amistades `ACCEPTED`.
- `spectatableMatch` es `null` si el amigo no tiene match `IN_PROGRESS` espectable.
- No incluye informacion privada del match.

### SpectatableMatchRef

Referencia minima de match reutilizable por la lista social y por eventos de actividad.

**Campos**:

- `id`: identificador del match.
- `status`: estado visible del match; para disponibilidad social se espera `IN_PROGRESS`.

**Reglas**:

- El `id` permite iniciar el flujo existente de spectate.
- No contiene cartas, acciones disponibles, puntaje de ronda ni jugadores privados.
- Si el match sale de `IN_PROGRESS`, la referencia debe removerse del estado social.

### FriendActivityState

Snapshot completo de actividad social para el jugador autenticado.

**Campos**:

- `friends`: lista de `FriendActivity`.

**Reglas**:

- Se calcula desde amistades aceptadas vigentes.
- Debe poder reemplazar el estado local completo del cliente.
- Se envia al suscribirse a novedades sociales y puede usarse despues de reconexion.

### FriendActivityChange

Delta de actividad para un unico amigo.

**Campos**:

- `friendUsername`: username del amigo cuyo estado cambio.
- `spectatableMatch`: nullable. Nueva referencia espectable o `null` si dejo de estar disponible.

**Reglas**:

- Se envia solo a usuarios que son amigos aceptados vigentes del jugador involucrado.
- Debe ser idempotente para el cliente: aplicar dos veces el mismo cambio deja el mismo estado.
- Si la amistad deja de estar aceptada, debe publicarse o quedar cubierto por un evento social que
  remueva la relacion de la vista.

### FriendActivityResolver

Servicio de aplicacion que arma actividad visible desde amistades aceptadas y matches activos.

**Entradas**:

- `PlayerId viewerId`: jugador autenticado que mira su lista.

**Salidas**:

- Lista de `FriendActivity`.
- `FriendActivityChange` para un par viewer/amigo cuando cambia un match.

**Reglas**:

- Usa solo amistades aceptadas.
- Resuelve como maximo una partida espectable activa por amigo.
- Si un usuario no esta registrado o no puede usar social, falla igual que los casos sociales
  existentes.

## Transiciones de estado

```text
Amigo sin partida espectable
  -> amigo entra a match IN_PROGRESS
  -> se publica FRIEND_ACTIVITY_CHANGED con spectatableMatch
  -> cliente muestra accion de spectate

Amigo con partida espectable
  -> match termina / se cancela / se abandona / se forfeitea
  -> se publica FRIEND_ACTIVITY_CHANGED con spectatableMatch null
  -> cliente oculta accion de spectate

Usuario se suscribe a social
  -> backend calcula FriendActivityState
  -> se publica FRIEND_ACTIVITY_STATE
  -> cliente reemplaza estado local de actividad

Amistad aceptada se elimina
  -> evento social existente remueve la amistad
  -> actividad futura no se publica para esa relacion
  -> si habia spectatableMatch visible, la vista social debe removerlo junto con el amigo
```

## Invariantes

- La actividad social no registra espectadores.
- La actividad social nunca muestra informacion privada de una partida.
- Un usuario solo recibe actividad de amistades aceptadas vigentes.
- Un amigo puede aparecer como maximo una vez en el snapshot.
- Un cambio de actividad reemplaza el estado anterior de ese amigo.
- La regla de truco-to-three no cambia: partida a exactamente 3 puntos, pasarse de 3 pierde, series
  mejor de 1, 3 o 5 cuando aplique.
