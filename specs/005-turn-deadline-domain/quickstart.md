# Quickstart — Verificación del deadline de turno

## Verificación automatizada

```bash
# Suite completa (incluye ArchUnit y coverage 70%)
./gradlew test

# Tests de dominio del agregado Match
./gradlew test --tests "com.villo.truco.domain.model.match.MatchTest"

# Exactitud temporal del forfeit (base para "mostrado == ejecutado")
./gradlew test --tests "com.villo.truco.integration.TimeoutExactnessMatchIT"

# Reconciliación al reiniciar
./gradlew test --tests "com.villo.truco.integration.TimeoutReconciliationIT"
```

Casos clave que deben quedar cubiertos (títulos en español, Principio III/IV):

- El dominio resetea el ancla y emite `ActionDeadlineSetEvent` al cambiar el asiento que debe actuar
  (cambio de turno, canto, respuesta, nueva ronda).
- El dominio emite `ActionDeadlineClearedEvent` cuando ningún asiento debe actuar.
- El evento derivado **no** consume `stateVersion` (llega con `null`).
- El `actionDeadline` proyectado coincide con el instante de forfeit ejecutado (INV-1).
- El `actionDeadlineSeat` coincide con el asiento penalizado por timeout (INV-2).
- Tras reconstrucción/reinicio, el deadline derivado apunta al mismo instante (SC-005).

## Verificación manual (local)

```bash
docker compose up -d
./gradlew bootRun
```

1. Crear y arrancar un match (REST o quick match).
2. `GET /api/matches/{matchId}` → confirmar que `roundGame` incluye `actionDeadline`,
   `turnDurationMillis` y `actionDeadlineSeat`, con el asiento correcto.
3. Conectar STOMP a `/user/queue/match`:
    - al cambiar el turno / cantar / responder → llega `ACTION_DEADLINE_SET` con `stateVersion`
      ausente (`null`).
    - al resolverse una mano → llega `ACTION_DEADLINE_CLEARED`.
4. Suscribir un espectador (`/user/queue/match-spectate`) → confirmar que recibe los mismos dos
   eventos y que el snapshot `/spectate` trae los tres campos, sin `myCards` ni `availableActions`.
5. Dejar vencer el plazo → confirmar que el `MATCH_FORFEITED` ocurre en el instante que indicaba
   `actionDeadline` (dentro de 1s, SC-001).
6. Reconectar el cliente a mitad de turno → confirmar que el `actionDeadline` leído sigue apuntando
   al mismo instante (SC-004).

## Criterio de aceptación de la documentación

- `docs/CONTRATOS_API.md` §9.5 ya **no** lista `ACTION_DEADLINE_SET` / `ACTION_DEADLINE_CLEARED`
  como transicionales; aparecen como derivados sin `stateVersion`.
