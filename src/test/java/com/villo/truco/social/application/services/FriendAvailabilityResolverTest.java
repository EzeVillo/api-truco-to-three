package com.villo.truco.social.application.services;

import static com.villo.truco.social.application.services.FriendActivityTestFixtures.acceptedFriendship;
import static com.villo.truco.social.application.services.FriendActivityTestFixtures.startedMatch;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.usecases.commands.PlayerAvailabilityChecker;
import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.rematch.RematchSession;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.QuickMatchQueuePort;
import com.villo.truco.domain.ports.RematchSessionRepository;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.dto.FriendAvailabilityStatus;
import com.villo.truco.social.application.dto.FriendBusyReason;
import com.villo.truco.social.application.ports.out.FriendOnlinePresencePort;
import com.villo.truco.social.domain.model.friendship.Friendship;
import com.villo.truco.social.domain.ports.FriendshipQueryRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FriendAvailabilityResolver")
class FriendAvailabilityResolverTest {

  private static FriendAvailabilityResolver resolver(final PlayerId viewer,
      final List<Friendship> friendships, final Map<PlayerId, String> usernames,
      final java.util.function.Function<PlayerId, Optional<Match>> matchResolver,
      final PlayerAvailabilityChecker availabilityChecker,
      final FriendOnlinePresencePort onlinePresencePort) {

    final var friendshipRepository = mock(FriendshipQueryRepository.class);
    when(friendshipRepository.findAcceptedByPlayer(viewer)).thenReturn(friendships);
    for (final var friendship : friendships) {
      when(friendshipRepository.findAcceptedByPlayer(friendship.counterpartOf(viewer))).thenReturn(
          List.of(friendship));
    }

    final var userRepository = mock(UserQueryRepository.class);
    when(userRepository.findUsernamesByIds(anySet())).thenAnswer(invocation -> {
      final java.util.Set<PlayerId> ids = invocation.getArgument(0);
      return usernames.entrySet().stream().filter(entry -> ids.contains(entry.getKey()))
          .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    });

    return new FriendAvailabilityResolver(friendshipRepository, new MatchRepo(matchResolver),
        userRepository, availabilityChecker, onlinePresencePort);
  }

  private static PlayerAvailabilityChecker availabilityChecker(final boolean unfinishedMatch,
      final boolean quickQueued) {

    return availabilityChecker(unfinishedMatch, quickQueued, false);
  }

  private static PlayerAvailabilityChecker availabilityChecker(final boolean unfinishedMatch,
      final boolean quickQueued, final boolean openRematch) {

    final var matchRepo = mock(MatchQueryRepository.class);
    when(matchRepo.hasUnfinishedMatch(any())).thenReturn(unfinishedMatch);
    when(matchRepo.hasActiveMatch(any())).thenReturn(unfinishedMatch);
    final var leagueRepo = mock(LeagueQueryRepository.class);
    when(leagueRepo.findInProgressByPlayer(any())).thenReturn(Optional.empty());
    when(leagueRepo.findWaitingByPlayer(any())).thenReturn(Optional.empty());
    final var cupRepo = mock(CupQueryRepository.class);
    when(cupRepo.findInProgressByPlayer(any())).thenReturn(Optional.empty());
    when(cupRepo.findWaitingByPlayer(any())).thenReturn(Optional.empty());
    final var botRegistry = mock(BotRegistry.class);
    final var rematchRepo = mock(RematchSessionRepository.class);
    when(rematchRepo.findOpenByPlayer(any())).thenReturn(
        openRematch ? Optional.of(mock(RematchSession.class)) : Optional.empty());
    final var quickMatchQueue = mock(QuickMatchQueuePort.class);
    when(quickMatchQueue.isPlayerQueued(any())).thenReturn(quickQueued);
    return new PlayerAvailabilityChecker(matchRepo, leagueRepo, cupRepo, botRegistry, rematchRepo,
        quickMatchQueue);
  }

  @Test
  @DisplayName("marca ocupado por match y conserva spectatableMatch")
  void marksBusyInMatchWithSpectatableMatch() {

    final var viewer = PlayerId.generate();
    final var friend = PlayerId.generate();
    final var rival = PlayerId.generate();
    final var match = startedMatch(friend, rival);
    final var resolver = resolver(viewer, List.of(acceptedFriendship(viewer, friend)),
        Map.of(friend, "martina"), player -> Optional.of(match), availabilityChecker(true, false),
        player -> false);

    final var state = resolver.resolveState(viewer);

    assertThat(state.friends()).singleElement().satisfies(friendState -> {
      assertThat(friendState.friendUsername()).isEqualTo("martina");
      assertThat(friendState.availability()).isEqualTo(FriendAvailabilityStatus.BUSY);
      assertThat(friendState.busyReason()).isEqualTo(FriendBusyReason.IN_MATCH);
      assertThat(friendState.spectatableMatch().id()).isEqualTo(match.getId().value().toString());
    });
  }

  @Test
  @DisplayName("marca ocupado por cola quick match aunque no tenga partida espectable")
  void marksBusyInQuickQueueWithoutSpectatableMatch() {

    final var viewer = PlayerId.generate();
    final var friend = PlayerId.generate();
    final var resolver = resolver(viewer, List.of(acceptedFriendship(viewer, friend)),
        Map.of(friend, "martina"), player -> Optional.empty(), availabilityChecker(false, true),
        player -> false);

    final var state = resolver.resolveState(viewer);

    assertThat(state.friends()).singleElement().satisfies(friendState -> {
      assertThat(friendState.availability()).isEqualTo(FriendAvailabilityStatus.BUSY);
      assertThat(friendState.busyReason()).isEqualTo(FriendBusyReason.IN_QUICK_QUEUE);
      assertThat(friendState.spectatableMatch()).isNull();
    });
  }

  @Test
  @DisplayName("online no cambia la disponibilidad para invitar")
  void onlineDoesNotChangeAvailability() {

    final var viewer = PlayerId.generate();
    final var friend = PlayerId.generate();
    final var resolver = resolver(viewer, List.of(acceptedFriendship(viewer, friend)),
        Map.of(friend, "martina"), player -> Optional.empty(), availabilityChecker(false, false),
        player -> true);

    final var state = resolver.resolveState(viewer);

    assertThat(state.friends()).singleElement().satisfies(friendState -> {
      assertThat(friendState.online()).isTrue();
      assertThat(friendState.availability()).isEqualTo(FriendAvailabilityStatus.AVAILABLE);
      assertThat(friendState.busyReason()).isNull();
    });
  }

  @Test
  @DisplayName("delta de disponibilidad recalcula bloqueos vigentes al cambiar un match")
  void availabilityDeltaRecalculatesCurrentBlockingReason() {

    final var viewer = PlayerId.generate();
    final var friend = PlayerId.generate();
    final var rival = PlayerId.generate();
    final var resolver = resolver(viewer, List.of(acceptedFriendship(viewer, friend)),
        Map.of(friend, "martina"), player -> Optional.empty(),
        availabilityChecker(false, false, true), player -> false);

    final var changes = resolver.resolveAvailabilityChangesByRecipient(friend, rival, friend);

    assertThat(changes).containsOnlyKeys(viewer);
    assertThat(changes.get(viewer)).satisfies(friendState -> {
      assertThat(friendState.friendUsername()).isEqualTo("martina");
      assertThat(friendState.availability()).isEqualTo(FriendAvailabilityStatus.BUSY);
      assertThat(friendState.busyReason()).isEqualTo(FriendBusyReason.OPEN_REMATCH);
      assertThat(friendState.spectatableMatch()).isNull();
    });
  }

  private record MatchRepo(
      java.util.function.Function<PlayerId, Optional<Match>> matchResolver) implements
      MatchQueryRepository {

    @Override
    public Optional<Match> findById(final MatchId matchId) {

      return Optional.empty();
    }

    @Override
    public boolean hasActiveMatch(final PlayerId playerId) {

      return false;
    }

    @Override
    public boolean hasUnfinishedMatch(final PlayerId playerId) {

      return false;
    }

    @Override
    public Optional<Match> findUnfinishedByPlayer(final PlayerId playerId) {

      return this.matchResolver.apply(playerId);
    }

    @Override
    public List<MatchId> findIdleMatchIds(final Instant idleSince) {

      return List.of();
    }

    @Override
    public List<Match> findPublicWaiting() {

      return List.of();
    }

    @Override
    public CursorPageResult<Match> findPublicWaiting(final CursorPageQuery pageQuery) {

      return new CursorPageResult<>(List.of(), null);
    }

  }

}
