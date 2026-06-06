# Data Model: Espectar partidas de amigos

## Entidades y modelos

### Spectatorship

Agregado existente que representa la sesión activa de espectador de un jugador.

**Campos relevantes**:

- `spectatorId`: jugador que mira.
- `activeMatchId`: match que está mirando, nullable si no está activo.

**Reglas**:

- Un jugador no puede espectar dos matches al mismo tiempo.
- Se registra al aceptar la suscripción de spectate.
- Se limpia al finalizar el match, al cortar conexión o al desuscribirse.

### SpectatingEligibilityPolicy

Política de dominio existente que decide si un `Spectatorship` puede empezar a mirar un `Match`.

**Reglas actuales que se mantienen**:

- El match debe estar `IN_PROGRESS`.
- El espectador no puede ser `playerOne` ni `playerTwo`.
- El espectador no puede tener otra sesión activa de spectate.
- La pertenencia a liga/copa sigue habilitando spectate.

**Regla nueva**:

- La amistad aceptada entre el espectador y al menos uno de los jugadores del match también habilita
  spectate.

### FriendshipSpectateEligibilityResolver

Puerto de dominio nuevo para consultar elegibilidad social sin acoplar el dominio de spectate al
bounded context social.

**Contrato conceptual**:

- Entrada: `Match` y `PlayerId spectatorId`.
- Salida: `true` si el espectador tiene amistad aceptada con `playerOne` o `playerTwo`.

**Validaciones**:

- Solo cuenta amistad aceptada.
- Solicitudes pendientes, rechazadas, canceladas o relaciones removidas devuelven `false`.
- El caso de partida propia ya lo bloquea `SpectatingEligibilityPolicy`.

### SocialFriendshipSpectateEligibilitySupport

Adapter de aplicación social que implementa el puerto anterior usando `FriendshipQueryRepository`.

**Relaciones**:

- Depende de `FriendshipQueryRepository`.
- Consulta `existsAcceptedByPlayers(spectatorId, playerOne)` y, si aplica,
  `existsAcceptedByPlayers(spectatorId, playerTwo)`.

### SpectatorCleanupOnFriendshipRemovedEventHandler

Handler de aplicación que reacciona al evento social `FRIENDSHIP_REMOVED`.

**Campos de entrada relevantes**:

- `requesterId`
- `addresseeId`

**Reglas**:

- Revisa si alguno de los dos participantes está espectando activamente.
- Si el match observado involucra al otro participante y el espectador ya no conserva elegibilidad
  por otro motivo válido, corta la sesión de spectate.
- Publica el cambio de conteo usando el ciclo de vida actual de spectatorship.

### FriendSummaryDTO

Modelo de lectura social existente que representa un amigo en `GET /api/social/friends`.

**Campos actuales**:

- `friendUsername`.

**Campos nuevos**:

- `spectatableMatch`: nullable. Referencia del match activo que el usuario puede intentar espectar.

### SpectatableMatchRefDTO

Referencia mínima para iniciar el flujo existente de spectate.

**Campos**:

- `id`: identificador del match.
- `status`: estado del match, esperado `IN_PROGRESS` para habilitar acción de espectar.

**Reglas**:

- Solo se expone para amigos confirmados.
- Solo se expone cuando el amigo tiene una partida `IN_PROGRESS`.
- No incluye cartas, marcador, nombres de ambos jugadores ni estado de ronda; esos datos llegan
  después por el flujo de spectate.

## Transiciones de estado

```text
Amigo sin partida espectable
  -> amigo entra a match IN_PROGRESS
  -> GET /api/social/friends devuelve spectatableMatch
  -> usuario se suscribe a /user/queue/match-spectate con matchId
  -> política valida amistad aceptada
  -> Spectatorship queda activo y se envía SPECTATE_STATE
  -> match termina / conexión corta / unsubscribe
  -> Spectatorship queda inactivo

Amistad aceptada habilita spectate activo
  -> amistad se elimina
  -> handler de FRIENDSHIP_REMOVED reevalúa elegibilidad
  -> si no queda liga/copa u otra regla válida
  -> Spectatorship queda inactivo
```

## Invariantes

- No existe opt-out de spectate para amigos confirmados.
- La vista de espectador nunca muestra cartas no jugadas ni eventos privados por asiento.
- La elegibilidad por amistad no elimina la elegibilidad existente por liga/copa.
- El snapshot REST de spectate solo funciona después de un alta WebSocket válida.
- Un jugador no puede espectar su propio match aunque aparezca en otra relación social o
  competición.
- Eliminar una amistad corta solo el acceso que dependía de esa amistad; no debe romper un spectate
  que siga habilitado por liga/copa.
