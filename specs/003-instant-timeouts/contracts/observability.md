# Contrato: Observabilidad de timeouts

## Logs

Nivel `INFO`, todos en español, con MDC `entityType` y `entityId`:

- `Timeout programado: entityType={}, entityId={}, deadlineUtc={}, etaSeconds={}`
- `Timeout cancelado: entityType={}, entityId={}, razón={}`
- `Timeout disparado: entityType={}, entityId={}, lagMs={}`
- `Timeout reconciliado al arrancar: total={}, vencidos={}, futuros={}`
- `Timeout red de seguridad: reprogramados={}, disparados={}`

## Métricas Micrometer

| Métrica                            | Tipo                      | Tags                                           | Descripción                                                                                        |
|------------------------------------|---------------------------|------------------------------------------------|----------------------------------------------------------------------------------------------------|
| `truco.timeout.scheduled`          | counter                   | `entityType`                                   | Veces que se programó un timeout (incluye reprogramaciones).                                       |
| `truco.timeout.cancelled`          | counter                   | `entityType`, `reason`                         | Veces que se canceló un timeout antes de disparar.                                                 |
| `truco.timeout.fired`              | counter                   | `entityType`, `outcome` (`applied`, `skipped`) | Veces que el handler ejecutó la acción. `skipped` cuando la entidad ya no estaba en estado válido. |
| `truco.timeout.lag`                | distribution summary (ms) | `entityType`                                   | Diferencia entre `deadline` y momento de inicio de la acción. Sirve para verificar SC-001.         |
| `truco.timeout.pending`            | gauge                     | `entityType`                                   | Cantidad actual de futures agendados in-memory.                                                    |
| `truco.timeout.reconcile.duration` | timer                     | `phase` (`startup`, `safety-net`)              | Duración de la reconciliación.                                                                     |

## Health indicator

Reemplazar la lógica actual de `SchedulerHeartbeatRegistry` que verifica "último heartbeat del
job":

- **UP** si el scheduler in-memory está aceptando tareas (el pool no está terminado) y la red de
  seguridad corrió hace ≤ 10 min.
- **DOWN** si el scheduler está terminado o la red de seguridad no corre hace > 10 min.
- Detalles del payload (atributos): `pendingByType`, `lastReconciliationAt`, `lastFireAt`.
