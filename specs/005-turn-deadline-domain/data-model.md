# Data Model — Deadline de turno

## Concepto central: el deadline es una proyección, no una entidad nueva

No se introduce ninguna entidad persistente nueva ni columna nueva. El deadline se **deriva** de
estado existente más configuración existente.

```
actionDeadline      = ancla + duración
ancla               = Match.lastActivityAt        (estado de dominio, ya persistido en last_activity_at)
duración            = idleTimeout                 (configuración de aplicación, MatchTimeoutProperties)
actionDeadlineSeat  = seatOf(Match.getCurrentTurn())   (conocimiento de dominio)
turnDurationMillis  = duración expresada en milisegundos
```

## Estado de dominio involucrado (existente)

| Atributo                 | Tipo       | Origen                                           | Cambio en esta feature                                                                                                                 |
|--------------------------|------------|--------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| `Match.lastActivityAt`   | `Instant`  | Ya existe; persistido en `last_activity_at` (V2) | Pasa a ser seteado por el **dominio** (`= now`) en cada transición que cambia quién debe actuar, en vez de estamparse en `@PreUpdate`. |
| `Match.getCurrentTurn()` | `PlayerId` | Ya existe                                        | Sin cambio; se reutiliza para derivar el asiento.                                                                                      |
| `idleTimeout`            | `Duration` | Config de aplicación                             | Sin cambio; se reutiliza como duración.                                                                                                |

## Reglas de transición del ancla (FR-004)

El ancla (`lastActivityAt`) se reinicia a `now` cuando cambia el asiento que debe actuar:

- Cambio de turno (jugar carta que pasa el turno).
- Canto de truco o envido (el deber de actuar pasa al rival que debe responder).
- Respuesta a un canto que devuelve el juego al rival.
- Inicio de nueva ronda.

El reloj se considera **detenido** (deadline = `null`, se emite `ACTION_DEADLINE_CLEARED`) cuando:

- `getCurrentTurn() == null` (mano resolviéndose, entre rondas, esperas del servidor).
- El match no está `IN_PROGRESS` (`FINISHED`, `CANCELLED`, etc.).

## Eventos derivados (nuevos, no consumen `stateVersion`)

| Evento                       | Payload (tras composición en aplicación)                     | Cuándo                                                             |
|------------------------------|--------------------------------------------------------------|--------------------------------------------------------------------|
| `ActionDeadlineSetEvent`     | `{ seat, actionDeadline (epochMillis), turnDurationMillis }` | Cada vez que (re)inicia la ventana porque cambió quién debe actuar |
| `ActionDeadlineClearedEvent` | `{}`                                                         | Cuando ningún asiento debe actuar                                  |

Nota de capas: el evento de **dominio** transporta `seat` y el **ancla**; la capa de **aplicación**
(`MatchNotificationEventTranslator`) compone `actionDeadline = ancla + idleTimeout` y agrega
`turnDurationMillis`. La duración nunca entra al dominio (D3).

## Validaciones / invariantes

- **INV-1 (fuente única)**: el valor `actionDeadline` expuesto en snapshot, en evento en vivo, en el
  scheduler de ejecución y en la reconciliación al reiniciar DEBE derivarse de la misma fórmula
  `ancla + idleTimeout`. No existe un segundo cómputo independiente.
- **INV-2 (coherencia asiento↔forfeit)**: el `actionDeadlineSeat` expuesto DEBE ser el mismo asiento
  que el dominio penalizaría por timeout en ese instante.
- **INV-3 (reloj detenido explícito)**: si no hay asiento que deba actuar, los tres campos se
  exponen
  como `null` y se emite `ACTION_DEADLINE_CLEARED` (no se deja un deadline viejo).
