# Quickstart / Verificación: Partidas Bot vs Bot Espectables

Cómo verificar la feature de punta a punta (backend). Requiere app corriendo
(`./gradlew bootRun`, con `docker compose up -d` para PostgreSQL) y un JWT de un usuario autenticado.

## 1. Listar bots y elegir dos

```bash
curl -s http://localhost:8080/api/bots -H "Authorization: Bearer $JWT" | jq
# Tomar dos botId distintos del catálogo.
```

## 2. Crear la partida bot-vs-bot

```bash
curl -s http://localhost:8080/api/matches/bot-vs-bot \
  -H "Authorization: Bearer $JWT" -H "Content-Type: application/json" \
  -d '{"botOneId":"<BOT_1>","botTwoId":"<BOT_2>","gamesToPlay":3}' | jq
# => { "matchId": "<MID>" }
```

Verificar:
- Mismo bot en ambos asientos → `400`.
- Bot inexistente → `404`.

## 3. La creación deja al usuario ocupado (sin mirar)

```bash
curl -s http://localhost:8080/api/me/presence -H "Authorization: Bearer $JWT" | jq
# => busy:true y ownedBotMatch:{ matchId:"<MID>", status:"IN_PROGRESS" }
```

Verificar ocupación total (sin haber abierto spectate):
- Crear otra bot-match → rechazado por ocupado.
- `POST /api/matches/bot`, `POST /api/matches/quick`, etc. → rechazados por ocupado.

## 4. Espectar viendo las cartas de ambos bots (solo el creador)

```bash
curl -s "http://localhost:8080/api/matches/<MID>/spectate" -H "Authorization: Bearer $JWT" | jq '.currentRound | {handPlayerOne, handPlayerTwo, playedHands}'
# => handPlayerOne y handPlayerTwo con cartas de cada bot.
```

Flujo WebSocket-first (real): suscribirse a `/user/queue/match-spectate` con header `matchId: <MID>`
→ llega `SPECTATE_STATE` con ambas manos. Al recibir `ROUND_STARTED`, re-consultar `GET /spectate`
para ver las manos recién repartidas.

Verificar owner-only:
- Con el JWT de **otro** usuario, intentar espectar `<MID>` → `422` (REST) / `SPECTATE_ERROR` (WS).

## 5. Avance automático

- Sin intervención humana, la serie progresa (los bots juegan solos) hasta una conclusión.
- Observar `playedHands`, `scorePlayerOne/Two`, `gamesWonPlayerOne/Two` cambiando entre consultas.

## 6. Liberación al terminar

```bash
# Una vez que el match llega a FINISHED:
curl -s http://localhost:8080/api/me/presence -H "Authorization: Bearer $JWT" | jq
# => busy:false y ownedBotMatch:null  → el creador puede crear otra partida.
```

## 7. Chequeos automatizados

```bash
./gradlew test            # incluye CleanArchitectureTest (ArchUnit) y contract tests
./gradlew build           # verifica cobertura JaCoCo ≥ 70%
```

Tests esperados (títulos en español):
- Crear bot-vs-bot: bots iguales / inexistente / usuario ocupado.
- `PlayerAvailabilityChecker`: `OWNS_BOT_MATCH` bloquea altas.
- `SpectatingEligibilityPolicy`: creador permitido / no-creador rechazado.
- `SpectatorMatchStateDTOAssembler`: manos presentes en bot-vs-bot, `null` en partidas con humanos.
- `BotVsBotRematchVeto`: vetea revancha de bot-vs-bot.
- `UserPresenceResolver`: `ownedBotMatch` presente con match activo, ausente al terminar.
- Adapter JPA del registro (H2): `register`/`isBotVsBotMatch`/`findOwnerByMatchId`/
  `findActiveOwnedMatchId`.
