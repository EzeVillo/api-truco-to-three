# Quickstart: Recopilación de partidas para entrenamiento de bots

**Feature**: 015-gameplay-recording | **Date**: 2026-06-15

Guía para verificar manualmente que la recopilación funciona. No hay endpoint ni tooling: la
verificación es por base de datos.

## Prerrequisitos

```bash
docker compose up -d        # PostgreSQL + Adminer
./gradlew bootRun           # API en http://localhost:8080
```

Adminer disponible en `http://localhost:8081` (o consultar PostgreSQL con cualquier cliente SQL).

## Verificación end-to-end

1. **Crear y jugar una partida contra un bot** (vía la app / REST como de costumbre): jugar cartas,
   cantar truco/envido, responder, e idealmente irse al mazo en alguna mano.

2. **Consultar el registro** de esa partida:

   ```sql
   SELECT state_version, actor_seat, actor_type, action_type, action_detail
   FROM match_action_log
   WHERE match_id = '<MATCH_ID>'
   ORDER BY state_version;
   ```

   **Esperado**:
    - Una fila por cada acción jugable (tuya y del bot).
    - `actor_type` alterna `HUMAN` / `BOT` según quién jugó.
    - `state_version` estrictamente creciente, **sin huecos ni repetidos**.
    - `action_type` y `action_detail` reflejan la acción real (p. ej. `PLAY_CARD` con la carta).

3. **Verificar el estado completo capturado** (incluye ambas manos, sin redactar):

   ```sql
   SELECT match_state -> 'currentRound' -> 'handPlayerOne' AS mano_uno,
          match_state -> 'currentRound' -> 'handPlayerTwo' AS mano_dos
   FROM match_action_log
   WHERE match_id = '<MATCH_ID>'
   ORDER BY state_version
   LIMIT 1;
   ```

   **Esperado**: ambas manos presentes (es estado server-side, nunca enviado al cliente).

## Verificaciones de criterios de aceptación

| Criterio                                 | Cómo verificar                                                                                                 |
|------------------------------------------|----------------------------------------------------------------------------------------------------------------|
| SC-001 (100% de acciones registradas)    | Comparar la cantidad de acciones jugadas con la cantidad de filas de la partida.                               |
| SC-002 (secuencia sin huecos/duplicados) | `state_version` consecutivo por `match_id`.                                                                    |
| SC-003 (sin impacto en el juego)         | La partida transcurre normal; sin latencia perceptible.                                                        |
| SC-005 (inmutabilidad)                   | Re-consultar tras más jugadas: las filas previas no cambian.                                                   |
| FR-005 (acción concreta)                 | `action_type` + `action_detail` por fila.                                                                      |
| FR-010 (fallo no afecta jugada)          | Test automatizado: forzar excepción del adapter y verificar que la jugada igual se completa y no hay rollback. |
| FR-013 (rechazos no registran)           | Intentar una acción inválida: no aparece fila nueva.                                                           |

## Pruebas automatizadas a incluir (resumen)

- **Unit (application)**: el decorator registra tras un `handle` exitoso; **traga y loguea** si el
  puerto lanza; deriva `actorSeat`/`actorType` correctamente; no registra si el delegate lanza.
- **Unit (application)**: `RecordedActionFactory` mapea cada uno de los 6 commands a su
  `RecordedActionType` y detalle.
- **Integration (infrastructure, H2)**: el adapter inserta append-only; `ON CONFLICT DO NOTHING`
  no duplica ante misma `(match_id, state_version)`.
- **Arquitectura**: `CleanArchitectureTest` (ArchUnit) sigue verde — domain sin Spring, puerto en
  `domain.ports`, decorator sin imports de Spring.

> Tras implementar: correr `./gradlew test` (incluye ArchUnit + cobertura ≥ 70%).
