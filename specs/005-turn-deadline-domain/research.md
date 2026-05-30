# Research — Deadline de turno como concepto de dominio

Decisiones de diseño resueltas antes de la Fase 1. Cada una en formato Decisión / Rationale /
Alternativas consideradas.

## D1 — Representación del deadline: proyección derivada (no estado absoluto persistido)

**Decisión**: El deadline absoluto **no** se persiste como dato propio. Se computa como
`ancla + duración`, donde el ancla es estado de dominio ya existente (`lastActivityAt`) y la
duración es configuración operativa (`idleTimeout`). La misma fórmula se usa en snapshot, evento en
vivo, scheduler de ejecución y reconciliación.

**Rationale**:

- Es la representación más honesta a nivel DDD: el deadline no es un hecho de negocio independiente,
  sino una función de "cuándo empezó la ventana" + "cuánto dura". Es análogo a cómo
  `availableActions`
  es una proyección, no estado.
- Reutiliza la columna `last_activity_at` (creada en `V2`) → **sin migración** (Principio V /
  YAGNI).
- Una sola fórmula = imposible que "lo mostrado" difiera de "lo ejecutado" (FR-003).

**Alternativas consideradas**:

- *Persistir el deadline absoluto en el agregado* (columna nueva + evento transicional): rechazada
  por costo de migración, por almacenar un dato derivable, y porque la duración es config operativa,
  no invariante de dominio. Además contradice la decisión del usuario de evento derivado.
- *Calcular el deadline dos veces (scheduler con `now+timeout`, snapshot
  con `lastActivityAt+timeout`)*:
  rechazada porque introduce divergencia (riesgo de forfeit "fantasma", viola FR-003/SC-003).

## D2 — Evento derivado, no transicional

**Decisión**: `ACTION_DEADLINE_SET` y `ACTION_DEADLINE_CLEARED` se emiten como `MatchDerivedEvent`.

**Rationale**:

- Decisión explícita del usuario.
- La infraestructura ya lo soporta sin cambios: `MatchNotificationEventTranslator` asigna
  `stateVersion = null` a todo `MatchDerivedEvent` (línea 53), y `Match.collectRoundEvents()` ya
  rutea
  derivados vía `addDerivedEvent` sin consumir `nextStateVersion()`.
- Evita contaminar el cursor de reconciliación del FE con una proyección de reloj. El deadline ya
  está implícito en los eventos transicionales que cambian quién debe actuar (`TURN_CHANGED`,
  `TRUCO_CALLED`, etc.).

**Alternativas consideradas**:

- *Transicional (como está hoy en la doc)*: rechazada; redundante con el cursor y mezcla
  presentación con secuencia de estado. **Consecuencia**: hay que corregir `docs/CONTRATOS_API.md`
  §9.5 (ver D6).

## D3 — Ubicación de la duración: configuración de aplicación, fuera del dominio

**Decisión**: La duración del turno (`idleTimeout`) permanece como propiedad de aplicación
(`MatchTimeoutProperties` / config). El **evento derivado de dominio lleva solo el asiento y el
ancla**; la capa de aplicación compone `actionDeadline = ancla + duración` y agrega
`turnDurationMillis`.

**Rationale**:

- FR-010: la duración es configurable a nivel operativo, no un invariante del truco. Meterla al
  dominio la congelaría o ensuciaría el agregado con config.
- Mantiene el dominio puro: decide *cuándo* empezó la ventana y *quién* debe actuar; la app aporta
  *cuánto dura* y proyecta.

**Alternativas consideradas**:

- *Duración como `MatchRules` del dominio*: aceptable pero la fija al crear el match y agrega
  estado;
  innecesario para el objetivo (YAGNI).

## D4 — El tiempo entra al dominio como valor (`Instant now`)

**Decisión**: Los métodos de acción del dominio que cambian "quién debe actuar" reciben
`Instant now`
y setean el ancla (`lastActivityAt = now`) y emiten el evento derivado. El `now` proviene del
`Clock`
ya existente en infraestructura, pasado a través del caso de uso.

**Rationale**:

- Principio II: el agregado no debe leer el reloj (no determinista, no testeable). Recibir `now`
  como
  valor preserva pureza y determinismo (mismo `now` → mismo resultado).
- Hoy `lastActivityAt` lo estampa la infra en `@PreUpdate` (`Instant.now()`); mover ese control al
  dominio hace que el **reset del reloj ocurra exactamente cuando cambia el asiento que debe actuar
  **
  (FR-004), no en cualquier `save`.

**Alternativas consideradas**:

- *Seguir estampando en `@PreUpdate`*: ancla acoplada al ciclo de persistencia, no a la transición
  de
  negocio; aceptable hoy porque casi todo `save` cambia el turno, pero menos preciso para FR-004 y
  deja el "cuándo" fuera del dominio. Se prefiere control de dominio.
- **Verificación requerida en implementación**: confirmar que el cambio de quién estampa el ancla no
  regresione la temporalidad de forfeit existente (cubierto por `TimeoutExactnessMatchIT`).

## D5 — Asiento que debe actuar (`actionDeadlineSeat`)

**Decisión**: `actionDeadlineSeat = seatOf(match.getCurrentTurn())`. El dominio ya usa
`getCurrentTurn()` para decidir el ganador por timeout (`determineTimeoutWinner`), por lo que ese
valor ya representa "quién debe actuar".

**Rationale**: Reutiliza la fuente de verdad existente del forfeit; garantiza que el asiento
mostrado
coincide con el penalizado.

**Verificación requerida en implementación**: confirmar que ante un **canto pendiente** (
truco/envido
esperando respuesta) `getCurrentTurn()` apunta al asiento que debe responder, no al del turno de
juego. Si la API expone un `currentTurn` de "turno de juego" distinto del responsable, documentar la
diferencia (la nota de §4.18 contempla que `actionDeadlineSeat` pueda no coincidir con
`currentTurn`).

## D6 — Corrección obligatoria del contrato documentado

**Decisión**: Actualizar `docs/CONTRATOS_API.md` §9.5 moviendo `ACTION_DEADLINE_SET` y
`ACTION_DEADLINE_CLEARED` desde "Eventos transicionales (consumen `stateVersion`)" a la sección de
eventos **derivados** (no avanzan `stateVersion`).

**Rationale**: La doc fue redactada anticipadamente declarándolos transicionales. La decisión D2 los
vuelve derivados; dejar la doc como está volvería falso el contrato y rompería la lógica de
detección
de huecos por `stateVersion` del FE. Coherente con el gate de documentación de la constitution.

## D7 — Fuente única para el scheduler de ejecución

**Decisión**: `MatchTimeoutEventHandler` maneja el reloj **por estado**: en `IN_PROGRESS` agenda
solo
con `ACTION_DEADLINE_SET` (en `ancla + idleTimeout`, tomando el ancla del evento) y cancela con
`ACTION_DEADLINE_CLEARED`; en estados pre-juego (`WAITING_FOR_PLAYERS`/`READY`) conserva el
`scheduleTimeoutFromNow(idleTimeout)` actual. La reconciliación (`MatchTimeoutReconciliationSource`)
ya usa `lastActivityAt + idleTimeout`, por lo que vivo, reinicio y mostrado quedan en una sola
fórmula.

**Rationale**: Cierra el último punto de divergencia entre mostrado y ejecutado (FR-003, SC-001,
SC-005) sin regresionar el timeout previo al juego (no hay asiento que deba actuar en lobby, así que
los eventos de deadline no se emiten ahí).

**Alternativas consideradas**:

- *Que el handler reaccione a todo evento y arme en `ancla + idleTimeout`*: rechazada porque el
  handler no posee el ancla (solo recibe el evento); tomarla del `ACTION_DEADLINE_SET` es más
  simple.

## D8 — Composición del deadline en el mapper, no en el translator

**Decisión**: La composición `actionDeadline = ancla + idleTimeout` y `turnDurationMillis` se hace
en
`MatchEventMapper` (inyectándole `idleTimeout`), no en `MatchNotificationEventTranslator`.

**Rationale**: El camino de jugador (`MatchNotificationEventTranslator`) y el de espectador
(`SpectatorNotificationEventTranslator`) **reutilizan el mismo `MatchEventMapper`**. Si la
composición viviera en el translator del jugador, el espectador recibiría un payload incompleto.
Centralizarla en el mapper da el payload correcto a ambos sin duplicar lógica.

## D9 — Ruteo automático: los eventos de deadline NO son `SeatTargetedEvent`

**Decisión**: `ActionDeadlineSetEvent`/`ClearedEvent` no implementan `SeatTargetedEvent`.

**Rationale**: `MatchRecipientResolver` entrega a ambos jugadores todo evento que no sea
`SeatTargetedEvent`, y `SpectatorNotificationEventTranslator` reenvía a espectadores todo lo que no
sea `SeatTargetedEvent`. Al no marcarlos como dirigidos por asiento, llegan a ambos jugadores y a
espectadores **sin tocar esos componentes** (son públicos; el `seat` viaja en el payload, no como
destinatario). Los privados (`PLAYER_HAND_UPDATED`, `AVAILABLE_ACTIONS_UPDATED`) sí son
`SeatTargetedEvent` y quedan excluidos del reenvío.
