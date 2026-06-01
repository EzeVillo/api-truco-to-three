package com.villo.truco.profile.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.villo.truco.profile.domain.ports.MatchAchievementTrackerRepository;
import com.villo.truco.profile.domain.ports.PlayerProfileRepository;
import com.villo.truco.profile.domain.ports.ProfileEventNotifier;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProfileAchievementTrackingService")
class ProfileAchievementTrackingServiceTest {

  @Test
  @DisplayName("procesa secuencia real de eventos y resetea tracker con GAME_STARTED")
  void processesEventSequenceAndResetsTrackerOnGameStarted() {

    final var userQueryRepository = mock(UserQueryRepository.class);
    final var trackerRepository = mock(MatchAchievementTrackerRepository.class);
    final var profileRepository = mock(PlayerProfileRepository.class);
    final var profileEventNotifier = mock(ProfileEventNotifier.class);

    final var trackerStore = new HashMap<MatchId, MatchAchievementTracker>();
    doAnswer(inv -> {
      trackerStore.put(inv.getArgument(0, MatchAchievementTracker.class).getId(),
          inv.getArgument(0));
      return null;
    }).when(trackerRepository).save(any());
    when(trackerRepository.findByMatchId(any())).thenAnswer(
        inv -> Optional.ofNullable(trackerStore.get(inv.getArgument(0, MatchId.class))));

    final var profileStore = new HashMap<PlayerId, PlayerProfile>();
    doAnswer(inv -> {
      profileStore.put(inv.getArgument(0, PlayerProfile.class).getId(), inv.getArgument(0));
      return null;
    }).when(profileRepository).save(any());
    when(profileRepository.findByPlayerId(any())).thenAnswer(
        inv -> Optional.ofNullable(profileStore.get(inv.getArgument(0, PlayerId.class))));

    final var service = new ProfileAchievementTrackingService(userQueryRepository,
        trackerRepository, profileRepository, new AchievementPolicy(), profileEventNotifier);
    final var matchId = MatchId.generate();
    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();

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
    service.handle(new MatchEventEnvelope(matchId, playerOne, playerTwo,
        new RoundStartedEvent(1, PlayerSeat.PLAYER_ONE)));
    service.handle(new ScoreChangedEvent(matchId, playerOne, playerTwo, 2, 2));
    service.handle(new MatchEventEnvelope(matchId, playerOne, playerTwo,
        new RoundStartedEvent(2, PlayerSeat.PLAYER_TWO)));
    service.handle(new ScoreChangedEvent(matchId, playerOne, playerTwo, 3, 2));

    final var playerProfile = profileRepository.findByPlayerId(playerOne).orElseThrow();
    assertThat(playerProfile.hasUnlocked(
        AchievementCode.WIN_GAME_FROM_2_2_WITHOUT_CALLS_IN_ROUND)).isTrue();
    verify(profileEventNotifier).publishDomainEvents(anyList());

    service.handle(new GameStartedEvent(matchId, playerOne, playerTwo, 2));

    final var tracker = trackerRepository.findByMatchId(matchId).orElseThrow();
    assertThat(tracker.getCurrentGameNumber()).isEqualTo(2);
    assertThat(tracker.getScorePlayerOne()).isZero();
    assertThat(tracker.getScorePlayerTwo()).isZero();
    assertThat(tracker.isRoundHadCalls()).isFalse();
  }

  @Test
  @DisplayName("misma secuencia contra bot desbloquea logros para el jugador humano")
  void sameSequenceAgainstBotUnlocksAchievements() {

    final var userQueryRepository = mock(UserQueryRepository.class);
    final var trackerRepository = mock(MatchAchievementTrackerRepository.class);
    final var profileRepository = mock(PlayerProfileRepository.class);
    final var profileEventNotifier = mock(ProfileEventNotifier.class);

    final var trackerStore = new HashMap<MatchId, MatchAchievementTracker>();
    doAnswer(inv -> {
      trackerStore.put(inv.getArgument(0, MatchAchievementTracker.class).getId(),
          inv.getArgument(0));
      return null;
    }).when(trackerRepository).save(any());
    when(trackerRepository.findByMatchId(any())).thenAnswer(
        inv -> Optional.ofNullable(trackerStore.get(inv.getArgument(0, MatchId.class))));

    final var profileStore = new HashMap<PlayerId, PlayerProfile>();
    doAnswer(inv -> {
      profileStore.put(inv.getArgument(0, PlayerProfile.class).getId(), inv.getArgument(0));
      return null;
    }).when(profileRepository).save(any());
    when(profileRepository.findByPlayerId(any())).thenAnswer(
        inv -> Optional.ofNullable(profileStore.get(inv.getArgument(0, PlayerId.class))));

    final var service = new ProfileAchievementTrackingService(userQueryRepository,
        trackerRepository, profileRepository, new AchievementPolicy(), profileEventNotifier);
    final var matchId = MatchId.generate();
    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();

    when(userQueryRepository.findUsernamesByIds(anySet())).thenAnswer(invocation -> {
      final Set<PlayerId> ids = invocation.getArgument(0);
      final var map = new HashMap<PlayerId, String>();
      if (ids.contains(playerOne)) {
        map.put(playerOne, "p1");
      }
      return map;
    });

    service.handle(new GameStartedEvent(matchId, playerOne, playerTwo, 1));
    service.handle(new MatchEventEnvelope(matchId, playerOne, playerTwo,
        new RoundStartedEvent(1, PlayerSeat.PLAYER_ONE)));
    service.handle(new ScoreChangedEvent(matchId, playerOne, playerTwo, 2, 2));
    service.handle(new MatchEventEnvelope(matchId, playerOne, playerTwo,
        new RoundStartedEvent(2, PlayerSeat.PLAYER_TWO)));
    service.handle(new ScoreChangedEvent(matchId, playerOne, playerTwo, 3, 2));

    assertThat(trackerRepository.findByMatchId(matchId)).isPresent();
    final var profile = profileRepository.findByPlayerId(playerOne).orElseThrow();
    assertThat(
        profile.hasUnlocked(AchievementCode.WIN_GAME_FROM_2_2_WITHOUT_CALLS_IN_ROUND)).isTrue();
    verify(profileEventNotifier).publishDomainEvents(anyList());
  }

}
