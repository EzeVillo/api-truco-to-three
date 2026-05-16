package com.villo.truco.profile.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.domain.model.match.events.MatchAbandonedEvent;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.profile.domain.model.PlayerStats;
import com.villo.truco.profile.domain.ports.PlayerStatsRepository;
import com.villo.truco.profile.domain.ports.ProcessedMatchStatsRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProfilePlayerStatsTrackingService")
class ProfilePlayerStatsTrackingServiceTest {

  private final BotRegistry botRegistry = mock(BotRegistry.class);
  private final UserQueryRepository userQueryRepository = mock(UserQueryRepository.class);
  private final InMemoryPlayerStatsRepository statsRepository = new InMemoryPlayerStatsRepository();
  private final InMemoryProcessedMatchStatsRegistry registry = new InMemoryProcessedMatchStatsRegistry();
  private final ProfilePlayerStatsTrackingService service = new ProfilePlayerStatsTrackingService(
      botRegistry, userQueryRepository, statsRepository, registry);

  @Test
  @DisplayName("MatchFinishedEvent acumula victoria y derrota para ambos jugadores")
  void matchFinishedAccumulatesOutcomes() {

    final var matchId = MatchId.generate();
    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();
    setupRegisteredPlayers(playerOne, playerTwo);
    seedStats(playerOne, playerTwo);

    service.handle(
        new MatchFinishedEvent(matchId, playerOne, playerTwo, PlayerSeat.PLAYER_ONE, 3, 1));

    assertThat(statsRepository.findByPlayerId(playerOne)).hasValueSatisfying(s -> {
      assertThat(s.matchesPlayed()).isEqualTo(1);
      assertThat(s.matchesWon()).isEqualTo(1);
      assertThat(s.matchesLost()).isEqualTo(0);
    });
    assertThat(statsRepository.findByPlayerId(playerTwo)).hasValueSatisfying(s -> {
      assertThat(s.matchesPlayed()).isEqualTo(1);
      assertThat(s.matchesWon()).isEqualTo(0);
      assertThat(s.matchesLost()).isEqualTo(1);
    });
  }

  @Test
  @DisplayName("MatchForfeitedEvent acumula victoria y derrota")
  void matchForfeitedAccumulatesOutcomes() {

    final var matchId = MatchId.generate();
    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();
    setupRegisteredPlayers(playerOne, playerTwo);
    seedStats(playerOne, playerTwo);

    service.handle(
        new MatchForfeitedEvent(matchId, playerOne, playerTwo, PlayerSeat.PLAYER_TWO, 1, 3));

    assertThat(statsRepository.findByPlayerId(playerTwo).orElseThrow().matchesWon()).isEqualTo(1);
    assertThat(statsRepository.findByPlayerId(playerOne).orElseThrow().matchesLost()).isEqualTo(1);
  }

  @Test
  @DisplayName("MatchAbandonedEvent acumula victoria y derrota; abandoner pierde")
  void matchAbandonedAbandoneGetLoss() {

    final var matchId = MatchId.generate();
    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();
    setupRegisteredPlayers(playerOne, playerTwo);
    seedStats(playerOne, playerTwo);

    service.handle(new MatchAbandonedEvent(matchId, playerOne, playerTwo, PlayerSeat.PLAYER_TWO,
        PlayerSeat.PLAYER_ONE, 1, 3));

    assertThat(statsRepository.findByPlayerId(playerOne).orElseThrow().matchesLost()).isEqualTo(1);
    assertThat(statsRepository.findByPlayerId(playerTwo).orElseThrow().matchesWon()).isEqualTo(1);
  }

  @Test
  @DisplayName("partida contra bot no afecta stats del humano")
  void botMatchSkipped() {

    final var matchId = MatchId.generate();
    final var human = PlayerId.generate();
    final var bot = PlayerId.generate();
    when(botRegistry.isBot(bot)).thenReturn(true);
    when(userQueryRepository.findUsernamesByIds(anySet())).thenAnswer(inv -> {
      final Set<PlayerId> ids = inv.getArgument(0);
      final Map<PlayerId, String> map = new HashMap<>();
      if (ids.contains(human)) {
        map.put(human, "human");
      }
      return map;
    });

    service.handle(new MatchFinishedEvent(matchId, human, bot, PlayerSeat.PLAYER_ONE, 3, 0));

    assertThat(statsRepository.findByPlayerId(human)).isEmpty();
  }

  @Test
  @DisplayName("replay del mismo evento no duplica stats")
  void replayDoesNotDuplicate() {

    final var matchId = MatchId.generate();
    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();
    setupRegisteredPlayers(playerOne, playerTwo);
    seedStats(playerOne, playerTwo);
    final var event = new MatchFinishedEvent(matchId, playerOne, playerTwo, PlayerSeat.PLAYER_ONE,
        3, 0);

    service.handle(event);
    service.handle(event);

    assertThat(statsRepository.findByPlayerId(playerOne).orElseThrow().matchesPlayed()).isEqualTo(
        1);
  }

  @Test
  @DisplayName("evento sin playerTwo es ignorado")
  void nullPlayerTwoIgnored() {

    final var matchId = MatchId.generate();
    final var playerOne = PlayerId.generate();

    service.handle(new MatchFinishedEvent(matchId, playerOne, null, PlayerSeat.PLAYER_ONE, 3, 0));

    assertThat(statsRepository.findByPlayerId(playerOne)).isEmpty();
  }

  private void seedStats(final PlayerId... players) {

    for (final PlayerId p : players) {
      statsRepository.save(PlayerStats.create(p));
    }
  }

  private void setupRegisteredPlayers(final PlayerId... players) {

    when(botRegistry.isBot(org.mockito.ArgumentMatchers.any())).thenReturn(false);
    when(userQueryRepository.findUsernamesByIds(anySet())).thenAnswer(inv -> {
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

  private static final class InMemoryPlayerStatsRepository implements PlayerStatsRepository {

    private final Map<PlayerId, PlayerStats> byId = new HashMap<>();

    @Override
    public void save(final PlayerStats stats) {

      this.byId.put(stats.getId(), stats);
    }

    @Override
    public Optional<PlayerStats> findByPlayerId(final PlayerId playerId) {

      return Optional.ofNullable(this.byId.get(playerId));
    }

  }

  private static final class InMemoryProcessedMatchStatsRegistry implements
      ProcessedMatchStatsRegistry {

    private final Map<String, Boolean> registered = new HashMap<>();

    @Override
    public boolean tryRegister(final PlayerId playerId, final MatchId matchId) {

      final var key = playerId.value() + ":" + matchId.value();
      return this.registered.putIfAbsent(key, true) == null;
    }

  }

}
