package com.villo.truco.history.application.usecases.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.domain.model.bot.BotProfile;
import com.villo.truco.domain.model.bot.valueobjects.BotPersonality;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.history.domain.model.MatchEndReason;
import com.villo.truco.history.domain.model.MatchHistoryEntry;
import com.villo.truco.history.domain.model.MatchOutcome;
import com.villo.truco.history.domain.model.PlayerMatchHistory;
import com.villo.truco.history.domain.ports.PlayerMatchHistoryRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GetPlayerMatchHistoryQueryHandler")
class GetPlayerMatchHistoryQueryHandlerTest {

  private final UserQueryRepository userQueryRepository = mock(UserQueryRepository.class);
  private final BotRegistry botRegistry = mock(BotRegistry.class);
  private final InMemoryRepository repository = new InMemoryRepository();
  private final GetPlayerMatchHistoryQueryHandler handler = new GetPlayerMatchHistoryQueryHandler(
      this.repository, this.userQueryRepository, this.botRegistry);

  @Test
  @DisplayName("historial inexistente devuelve lista vacía")
  void emptyWhenNoHistory() {

    final var player = PlayerId.generate();

    final var dto = this.handler.handle(new GetPlayerMatchHistoryQuery(player.value().toString()));

    assertThat(dto.entries()).isEmpty();
  }

  @Test
  @DisplayName("resuelve el username de un rival humano")
  void resolvesHumanOpponentName() {

    final var player = PlayerId.generate();
    final var opponent = PlayerId.generate();
    seedHistory(player, opponent, MatchOutcome.WON);
    when(this.botRegistry.isBot(opponent)).thenReturn(false);
    when(this.userQueryRepository.findUsernamesByIds(anySet())).thenReturn(
        Map.of(opponent, "rival123"));

    final var dto = this.handler.handle(new GetPlayerMatchHistoryQuery(player.value().toString()));

    assertThat(dto.entries()).singleElement().satisfies(e -> {
      assertThat(e.opponentName()).isEqualTo("rival123");
      assertThat(e.opponentIsBot()).isFalse();
      assertThat(e.outcome()).isEqualTo("WON");
    });
  }

  @Test
  @DisplayName("resuelve el displayName de un rival bot")
  void resolvesBotOpponentName() {

    final var player = PlayerId.generate();
    final var bot = PlayerId.generate();
    seedHistory(player, bot, MatchOutcome.LOST);
    when(this.botRegistry.isBot(bot)).thenReturn(true);
    when(this.botRegistry.getProfile(bot)).thenReturn(
        Optional.of(new BotProfile(bot, "Don Truco", new BotPersonality(50, 50, 50, 50, 50))));
    when(this.userQueryRepository.findUsernamesByIds(anySet())).thenReturn(new HashMap<>());

    final var dto = this.handler.handle(new GetPlayerMatchHistoryQuery(player.value().toString()));

    assertThat(dto.entries()).singleElement().satisfies(e -> {
      assertThat(e.opponentName()).isEqualTo("Don Truco");
      assertThat(e.opponentIsBot()).isTrue();
    });
  }

  private void seedHistory(final PlayerId player, final PlayerId opponent,
      final MatchOutcome outcome) {

    final var history = PlayerMatchHistory.create(player);
    history.record(
        new MatchHistoryEntry(MatchId.generate(), opponent, outcome, MatchEndReason.FINISHED, 3, 1,
            Instant.parse("2026-06-15T12:30:00Z")));
    this.repository.save(history);
  }

  private static final class InMemoryRepository implements PlayerMatchHistoryRepository {

    private final Map<PlayerId, PlayerMatchHistory> byId = new HashMap<>();

    @Override
    public void save(final PlayerMatchHistory history) {

      this.byId.put(history.getId(), history);
    }

    @Override
    public Optional<PlayerMatchHistory> findByPlayerId(final PlayerId playerId) {

      return Optional.ofNullable(this.byId.get(playerId));
    }

  }

}
