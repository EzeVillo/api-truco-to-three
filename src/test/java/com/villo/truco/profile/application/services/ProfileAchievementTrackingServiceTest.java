package com.villo.truco.profile.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.domain.model.match.events.GameStartedEvent;
import com.villo.truco.domain.model.match.events.MatchEventEnvelope;
import com.villo.truco.domain.model.match.events.RoundStartedEvent;
import com.villo.truco.domain.model.match.events.ScoreChangedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.profile.domain.model.AchievementCode;
import com.villo.truco.profile.domain.model.AchievementPolicy;
import com.villo.truco.profile.domain.model.MatchAchievementTracker;
import com.villo.truco.profile.domain.model.PlayerProfile;
import com.villo.truco.profile.domain.model.events.AchievementUnlocked;
import com.villo.truco.profile.domain.ports.MatchAchievementTrackerRepository;
import com.villo.truco.profile.domain.ports.PlayerProfileRepository;
import com.villo.truco.profile.domain.ports.ProfileEventNotifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProfileAchievementTrackingService")
class ProfileAchievementTrackingServiceTest {

  @Test
  @DisplayName("procesa secuencia real de eventos y resetea tracker con GAME_STARTED")
  void processesEventSequenceAndResetsTrackerOnGameStarted() {

    final var botRegistry = mock(BotRegistry.class);
    final var userQueryRepository = mock(UserQueryRepository.class);
    final var trackerRepository = new InMemoryMatchAchievementTrackerRepository();
    final var profileRepository = new InMemoryPlayerProfileRepository();
    final var profileEventNotifier = new CapturingProfileEventNotifier();
    final var service = new ProfileAchievementTrackingService(botRegistry, userQueryRepository,
        trackerRepository, profileRepository, new AchievementPolicy(), profileEventNotifier);
    final var matchId = MatchId.generate();
    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();

    when(botRegistry.isBot(playerOne)).thenReturn(false);
    when(botRegistry.isBot(playerTwo)).thenReturn(false);
    when(userQueryRepository.findUsernamesByIds(anySet())).thenAnswer(invocation -> {
      final Set<PlayerId> ids = invocation.getArgument(0);
      final var map = new HashMap<PlayerId, String>();
      if (ids.contains(playerOne)) {
        map.put(playerOne, "p1");
      }
      if (ids.contains(playerTwo)) {
        map.put(playerTwo, "p2");
      }
      return map;
    });

    service.handle(new GameStartedEvent(matchId, playerOne, playerTwo, 1));
    service.handle(
        new MatchEventEnvelope(matchId, playerOne, playerTwo,
            new RoundStartedEvent(1, PlayerSeat.PLAYER_ONE)));
    service.handle(new ScoreChangedEvent(matchId, playerOne, playerTwo, 2, 2));
    service.handle(
        new MatchEventEnvelope(matchId, playerOne, playerTwo,
            new RoundStartedEvent(2, PlayerSeat.PLAYER_TWO)));
    service.handle(new ScoreChangedEvent(matchId, playerOne, playerTwo, 3, 2));

    final var playerProfile = profileRepository.findByPlayerId(playerOne).orElseThrow();
    assertThat(playerProfile.hasUnlocked(AchievementCode.WIN_AT_2_2_WITHOUT_CALLS_IN_ROUND)).isTrue();
    assertThat(profileEventNotifier.events).hasSize(1);

    service.handle(new GameStartedEvent(matchId, playerOne, playerTwo, 2));

    final var tracker = trackerRepository.findByMatchId(matchId).orElseThrow();
    assertThat(tracker.getCurrentGameNumber()).isEqualTo(2);
    assertThat(tracker.getScorePlayerOne()).isZero();
    assertThat(tracker.getScorePlayerTwo()).isZero();
    assertThat(tracker.isRoundHadCalls()).isFalse();
  }

  @Test
  @DisplayName("misma secuencia contra bot no desbloquea logros")
  void sameSequenceAgainstBotDoesNotUnlock() {

    final var botRegistry = mock(BotRegistry.class);
    final var userQueryRepository = mock(UserQueryRepository.class);
    final var trackerRepository = new InMemoryMatchAchievementTrackerRepository();
    final var profileRepository = new InMemoryPlayerProfileRepository();
    final var profileEventNotifier = new CapturingProfileEventNotifier();
    final var service = new ProfileAchievementTrackingService(botRegistry, userQueryRepository,
        trackerRepository, profileRepository, new AchievementPolicy(), profileEventNotifier);
    final var matchId = MatchId.generate();
    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();

    when(botRegistry.isBot(playerOne)).thenReturn(false);
    when(botRegistry.isBot(playerTwo)).thenReturn(true);

    service.handle(new GameStartedEvent(matchId, playerOne, playerTwo, 1));
    service.handle(
        new MatchEventEnvelope(matchId, playerOne, playerTwo,
            new RoundStartedEvent(1, PlayerSeat.PLAYER_ONE)));
    service.handle(new ScoreChangedEvent(matchId, playerOne, playerTwo, 3, 2));

    assertThat(trackerRepository.findByMatchId(matchId)).isEmpty();
    assertThat(profileRepository.findByPlayerId(playerOne)).isEmpty();
    assertThat(profileEventNotifier.events).isEmpty();
  }

  private static final class InMemoryPlayerProfileRepository implements PlayerProfileRepository {

    private final Map<PlayerId, PlayerProfile> byId = new HashMap<>();

    @Override
    public void save(final PlayerProfile profile) {

      this.byId.put(profile.getId(), profile);
    }

    @Override
    public Optional<PlayerProfile> findByPlayerId(final PlayerId playerId) {

      return Optional.ofNullable(this.byId.get(playerId));
    }
  }

  private static final class InMemoryMatchAchievementTrackerRepository implements
      MatchAchievementTrackerRepository {

    private final Map<MatchId, MatchAchievementTracker> byId = new HashMap<>();

    @Override
    public void save(final MatchAchievementTracker tracker) {

      this.byId.put(tracker.getId(), tracker);
    }

    @Override
    public Optional<MatchAchievementTracker> findByMatchId(final MatchId matchId) {

      return Optional.ofNullable(this.byId.get(matchId));
    }

    @Override
    public void deleteByMatchId(final MatchId matchId) {

      this.byId.remove(matchId);
    }
  }

  private static final class CapturingProfileEventNotifier implements ProfileEventNotifier {

    private final List<AchievementUnlocked> events = new ArrayList<>();

    @Override
    public void publishDomainEvents(final List<? extends AchievementUnlocked> events) {

      this.events.addAll(events);
    }
  }
}
