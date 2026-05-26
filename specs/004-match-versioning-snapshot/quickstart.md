# Quickstart — Verificar versionado de Match

Pasos para validar manualmente las US 1–4 del spec.

## Prerrequisitos

```bash
docker compose up -d
./gradlew bootRun
```

API en `http://localhost:8080`. Swagger en `/swagger-ui/index.html`.

Tener dos JWT válidos (jugador A y jugador B). Conseguirlos por el flujo de auth habitual.

## US 1 — Conexión inicial sin duplicar/perder

1. **A** crea un match: `POST /api/matches` con body
   `{ "gamesToPlay": 3, "visibility": "PRIVATE" }`.
    - Anotar `matchId` y `joinCode`.
2. **B** se une: `POST /api/matches/{matchId}/join?joinCode=...`.
3. **A** y **B** marcan ready: `POST /api/matches/{matchId}/start` (ambos). El backend dispara
   `GAME_STARTED`, `ROUND_STARTED`, `HAND_DEALT`, etc.
4. **A** **demora intencionalmente** la suscripción al WS y hace
   `GET /api/matches/{matchId}` → debe traer `stateVersion: N` (N > 0).
5. **A** se suscribe a `/user/queue/match` y `/user/queue/match-derived`. Recibirá:
    - Posiblemente eventos retransmitidos con `stateVersion <= N` (descartar como duplicados o
      ya-en-snapshot).
    - Próximos eventos con `stateVersion = N+1, N+2, ...`.
6. **Validación**: la secuencia aplicada por el cliente (snapshot + eventos `> N`) coincide
   con el estado real del servidor.

## US 2 — Reconexión sin desincronización

1. Continuando del paso anterior, **A** cierra la conexión WS.
2. Mientras está offline, **B** juega una carta (`POST /api/matches/{matchId}/play-card`).
3. **A** reconecta y recibe por WS un evento con `stateVersion = lastApplied + 2` (hueco).
4. **A** detecta el hueco y solicita `GET /api/matches/{matchId}` nuevamente.
5. **A** aplica el snapshot y descarta del buffer cualquier evento con
   `stateVersion <= snapshot.stateVersion`.
6. **Validación**: estado final del cliente coincide con el del servidor; sin doble aplicación,
   sin huecos.

## US 3 — Información oculta con misma secuencia

1. En cualquier punto en que el servidor reparta cartas, observar simultáneamente los streams
   `/user/queue/match` de **A** y **B**.
2. Ambos reciben un evento `eventType: "HAND_DEALT"` con el **mismo `stateVersion`**.
3. **A** ve sólo sus cartas; **B** ve sólo las suyas. Ningún payload contiene campos del
   oponente (ni cantidad, ni `null`).
4. **Validación**: igual `stateVersion`, igual `eventType`, payloads distintos sin filtración
   indirecta.

## US 4 — Notificaciones no transicionales

1. Tras un cambio de turno, observar `/user/queue/match-derived` de **A**:
    - Debe llegar `AVAILABLE_ACTIONS_UPDATED` **sin** campo `stateVersion`.
2. Verificar que **B** no observa hueco en su secuencia transicional aunque no haya recibido
   ese mensaje (porque el derivado no estaba dirigido a él y, además, no consume
   `stateVersion`).

## Pruebas de regresión rápidas

```bash
./gradlew test --tests "com.villo.truco.domain.model.match.MatchStateVersionTest"
./gradlew test --tests "com.villo.truco.application.eventhandlers.MatchNotificationEventTranslatorTest"
./gradlew test --tests "com.villo.truco.infrastructure.http.MatchControllerSnapshotVersionTest"
./gradlew test --tests "com.villo.truco.infrastructure.websocket.MatchWsStreamVersionTest"
```

Build completo con coverage:

```bash
./gradlew build
```
