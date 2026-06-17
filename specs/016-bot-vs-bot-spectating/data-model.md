# Data Model: Partidas Bot vs Bot Espectables

## 1. Persistencia nueva

### Tabla `bot_vs_bot_matches` (registro de autoría)

Espeja a `campaign_matches`. Mapea cada partida bot-vs-bot a su dueño (creador).

```sql
-- V22__create_bot_vs_bot_matches.sql
CREATE TABLE bot_vs_bot_matches
(
    match_id UUID PRIMARY KEY,
    owner_id UUID NOT NULL
);

CREATE INDEX idx_bot_vs_bot_matches_owner_id ON bot_vs_bot_matches (owner_id);
```

| Columna    | Tipo | Notas                                                         |
|------------|------|---------------------------------------------------------------|
| `match_id` | UUID | PK. Identifica la partida bot-vs-bot.                         |
| `owner_id` | UUID | Usuario creador/dueño. Indexado para resolver "match propio". |

- No se borra al terminar el match: "activo" se deriva del estado del match (ver consultas).
- Sin FK dura a `matches` (igual que `campaign_matches`), para no acoplar el ciclo de borrado.

## 2. Puerto de dominio nuevo

`com.villo.truco.domain.ports.BotVsBotMatchRegistry`

```java
public interface BotVsBotMatchRegistry {

  void register(MatchId matchId, PlayerId ownerId);

  boolean isBotVsBotMatch(MatchId matchId);

  Optional<PlayerId> findOwnerByMatchId(MatchId matchId);

  Optional<MatchId> findActiveOwnedMatchId(PlayerId ownerId);

}
```

- `isBotVsBotMatch`: `existsById(matchId)`.
- `findOwnerByMatchId`: lookup por PK.
- `findActiveOwnedMatchId`: query que cruza con `matches` y filtra estados terminales
  (`FINISHED`, `FORFEITED`, `ABANDONED`, `CANCELLED`). Devuelve ≤ 1 fila.

## 3. Entidades del dominio (reutilizadas, sin cambios)

- **Match** (`domain.model.match.Match`): se crea con `createReady(botOne, botTwo, rules)`
  (`Visibility.PRIVATE`, `MatchStatus.READY` → `IN_PROGRESS` tras `startMatch`). **Sin cambios** al
  agregado. Métodos reutilizados: `getCardsOf(PlayerId)` (mano restante por asiento),
  `getPlayerOne`,
  `getPlayerTwo`, `getStatus`.
- **BotProfile** / **BotRegistry**: catálogo de bots; valida existencia de cada bot.
- **Spectatorship**: reutilizado para el espectado en vivo (no para la ocupación).

## 4. Comandos / DTOs nuevos

```java
// application.commands
public record CreateBotVsBotMatchCommand(PlayerId ownerId, GamesToPlay gamesToPlay,
                                         PlayerId botOneId, PlayerId botTwoId) {

  public CreateBotVsBotMatchCommand(String ownerId, int gamesToPlay, String botOneId,
      String botTwoId) {

    this(PlayerId.of(ownerId), GamesToPlay.of(gamesToPlay), PlayerId.of(botOneId),
        PlayerId.of(botTwoId));
  }

}

// application.dto
public record CreateBotVsBotMatchDTO(String matchId) {

}

public record ActiveOwnedBotMatchRefDTO(String matchId, String status) {

}
```

```java
// application.ports.in
public interface CreateBotVsBotMatchUseCase extends
    UseCase<CreateBotVsBotMatchCommand, CreateBotVsBotMatchDTO> {

}
```

## 5. DTOs modificados (deltas)

### `SpectatorRoundStateDTO` (+ manos para bot-vs-bot)

Se agregan dos campos al final; `null` para partidas con humanos (contrato existente intacto):

```java
public record SpectatorRoundStateDTO(String status, String currentTurn, String roundStatus,
                                     String currentTrucoCall, String currentEnvidoCall,
                                     String matchWinner, List<PlayedHandDTO> playedHands,
                                     CurrentHandDTO currentHand, Long actionDeadline,
                                     Long turnDurationMillis, String actionDeadlineSeat,
                                     List<CardDTO> handPlayerOne,   // NUEVO — solo bot-vs-bot
                                     List<CardDTO> handPlayerTwo) { // NUEVO — solo bot-vs-bot

}
```

### `UserPresenceDTO` y `UserPresenceResponse` (+ `ownedBotMatch`)

```java
public record UserPresenceDTO(boolean busy, ActiveMatchRefDTO match, ActiveLeagueRefDTO league,
                              ActiveCupRefDTO cup, ActiveRematchRefDTO rematch,
                              ActiveQuickMatchRefDTO quickMatch, ActiveSpectatingRefDTO spectating,
                              ActiveOwnedBotMatchRefDTO ownedBotMatch) { // NUEVO
  // busy = true si cualquiera de las refs (incluida ownedBotMatch) no es null
}
```

`UserPresenceResponse` agrega el record anidado `OwnedBotMatchRef { matchId, status }` y lo mapea
desde `ActiveOwnedBotMatchRefDTO`.

## 6. Excepciones nuevas

- `domain.model.match.exceptions.PlayerOwnsActiveBotMatchException` — motivo de ocupación
  `OWNS_BOT_MATCH`. Mapea a `409`/`422` en el handler HTTP (alineado con
  `PlayerAlreadyInActiveMatchException`).
- `domain.model.spectator.exceptions.SpectateBotMatchNotOwnerException` — espectado por no-creador.
  Mapea a `422` (`SPECTATE_ERROR` en WS).

## 7. Estados y transiciones

La partida bot-vs-bot reutiliza el ciclo de vida estándar de `Match`:

```
READY ──startMatch×2──▶ IN_PROGRESS ──(serie decidida)──▶ FINISHED
                                   └─(forfeit/abandon/cancel)─▶ terminal
```

Ocupación del dueño (derivada, no almacenada como estado):

```
crear ▶ owner ocupado (findActiveOwnedMatchId presente)
match IN_PROGRESS ▶ ocupado
match terminal ▶ libre (findActiveOwnedMatchId vacío)
```
