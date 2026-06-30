package com.villo.truco.history.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.domain.model.match.events.GameStartedEvent;
import com.villo.truco.domain.model.match.events.MatchAbandonedEvent;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.history.domain.model.MatchEndReason;
import com.villo.truco.history.domain.model.MatchOutcome;
import com.villo.truco.history.domain.model.PlayerMatchHistory;
import com.villo.truco.history.domain.ports.PlayerMatchHistoryRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MatchHistoryTrackingService")
class MatchHistoryTrackingServiceTest {

  private final UserQueryRepository userQueryRepository = mock(UserQueryRepository.class);
  private final InMemoryPlayerMatchHistoryRepository repository = new InMemoryPlayerMatchHistoryRepository();
  private final MatchHistoryTrackingService service = new MatchHistoryTrackingService(
      this.userQueryRepository, this.repository);

  @Test
  @DisplayName("MatchFinishedEvent registra una entrada por jugador, desde su perspectiva")
  void finishedRecordsBothPerspectives() {

    final var matchId = MatchId.generate();
    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();
    registerPlayers(playerOne, playerTwo);

    this.service.handle(
        new MatchFinishedEvent(matchId, playerOne, playerTwo, PlayerSeat.PLAYER_ONE, 3, 1));

    final var oneEntry = this.repository.findByPlayerId(playerOne).orElseThrow().getEntries()
        .getFirst();
    assertThat(oneEntry.opponentId()).isEqualTo(playerTwo);
    assertThat(oneEntry.outcome()).isEqualTo(MatchOutcome.WON);
    assertThat(oneEntry.endReason()).isEqualTo(MatchEndReason.FINISHED);
    assertThat(oneEntry.ownGamesWon()).isEqualTo(3);
    assertThat(oneEntry.opponentGamesWon()).isEqualTo(1);

    final var twoEntry = this.repository.findByPlayerId(playerTwo).orElseThrow().getEntries()
        .getFirst();
    assertThat(twoEntry.opponentId()).isEqualTo(playerOne);
    assertThat(twoEntry.outcome()).isEqualTo(MatchOutcome.LOST);
    assertThat(twoEntry.ownGamesWon()).isEqualTo(1);
    assertThat(twoEntry.opponentGamesWon()).isEqualTo(3);
  }

  @Test
  @DisplayName("MatchForfeitedEvent registra el motivo FORFEITED")
  void forfeitedRecordsReason() {

    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();
    registerPlayers(playerOne, playerTwo);

    this.service.handle(
        new MatchForfeitedEvent(MatchId.generate(), playerOne, playerTwo, PlayerSeat.PLAYER_TWO, 1,
            3));

    assertThat(
        this.repository.findByPlayerId(playerTwo).orElseThrow().getEntries().get(0)).satisfies(
        e -> {
          assertThat(e.outcome()).isEqualTo(MatchOutcome.WON);
          assertThat(e.endReason()).isEqualTo(MatchEndReason.FORFEITED);
        });
  }

  @Test
  @DisplayName("MatchAbandonedEvent registra el motivo ABANDONED")
  void abandonedRecordsReason() {

    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();
    registerPlayers(playerOne, playerTwo);

    this.service.handle(
        new MatchAbandonedEvent(MatchId.generate(), playerOne, playerTwo, PlayerSeat.PLAYER_ONE,
            PlayerSeat.PLAYER_TWO, 3, 0));

    assertThat(
        this.repository.findByPlayerId(playerOne).orElseThrow().getEntries().get(0)).satisfies(
        e -> {
          assertThat(e.outcome()).isEqualTo(MatchOutcome.WON);
          assertThat(e.endReason()).isEqualTo(MatchEndReason.ABANDONED);
        });
  }

  @Test
  @DisplayName("solo crea historial para jugadores registrados; el rival bot no obtiene historial")
  void doesNotCreateHistoryForBots() {

    final var human = PlayerId.generate();
    final var bot = PlayerId.generate();
    when(this.userQueryRepository.findUsernamesByIds(anySet())).thenAnswer(inv -> {
      final Set<PlayerId> ids = inv.getArgument(0);
      final Map<PlayerId, String> map = new HashMap<>();
      if (ids.contains(human)) {
        map.put(human, "human");
      }
      return map;
    });

    this.service.handle(
        new MatchFinishedEvent(MatchId.generate(), human, bot, PlayerSeat.PLAYER_ONE, 3, 0));

    assertThat(this.repository.findByPlayerId(human)).isPresent();
    assertThat(this.repository.findByPlayerId(bot)).isEmpty();
  }

  @Test
  @DisplayName("eventos no finales son ignorados")
  void ignoresNonFinalEvents() {

    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();
    registerPlayers(playerOne, playerTwo);

    this.service.handle(new GameStartedEvent(MatchId.generate(), playerOne, playerTwo, 1));

    assertThat(this.repository.findByPlayerId(playerOne)).isEmpty();
  }

  @Test
  @DisplayName("doble entrega del mismo evento no duplica la entrada")
  void idempotentOnReplay() {

    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();
    registerPlayers(playerOne, playerTwo);
    final var event = new MatchFinishedEvent(MatchId.generate(), playerOne, playerTwo,
        PlayerSeat.PLAYER_ONE, 3, 0);

    this.service.handle(event);
    this.service.handle(event);

    assertThat(this.repository.findByPlayerId(playerOne).orElseThrow().getEntries()).hasSize(1);
  }

  @Test
  @DisplayName("evento sin playerTwo es ignorado")
  void ignoresNullPlayerTwo() {

    final var playerOne = PlayerId.generate();

    this.service.handle(
        new MatchFinishedEvent(MatchId.generate(), playerOne, null, PlayerSeat.PLAYER_ONE, 3, 0));

    assertThat(this.repository.findByPlayerId(playerOne)).isEmpty();
  }

  private void registerPlayers(final PlayerId... players) {

    when(this.userQueryRepository.findUsernamesByIds(anySet())).thenAnswer(inv -> {
      final Set<PlayerId> ids = inv.getArgument(0);
      final Map<PlayerId, String> map = new HashMap<>();
      for (final PlayerId p : players) {
        if (ids.contains(p)) {
          map.put(p, p.value().toString().substring(0, 8));
        }
      }
      return map;
    });
  }

  private static final class InMemoryPlayerMatchHistoryRepository implements
      PlayerMatchHistoryRepository {

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
