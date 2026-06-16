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
import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.league.League;
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
import com.villo.truco.social.domain.model.invitation.ResourceInvitation;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationTargetType;
import com.villo.truco.social.domain.ports.FriendshipQueryRepository;
import com.villo.truco.social.domain.ports.ResourceInvitationQueryRepository;
import com.villo.truco.testutil.NoOpSpectatorshipRepository;
import java.time.Duration;
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

    return resolver(viewer, friendships, usernames, matchResolver, availabilityChecker,
        onlinePresencePort, mock(ResourceInvitationQueryRepository.class));
  }

  private static FriendAvailabilityResolver resolver(final PlayerId viewer,
      final List<Friendship> friendships, final Map<PlayerId, String> usernames,
      final java.util.function.Function<PlayerId, Optional<Match>> matchResolver,
      final PlayerAvailabilityChecker availabilityChecker,
      final FriendOnlinePresencePort onlinePresencePort,
      final ResourceInvitationQueryRepository resourceInvitationQueryRepository) {

    final var friendshipRepository = mock(FriendshipQueryRepository.class);
    when(friendshipRepository.findAcceptedByPlayer(viewer)).thenReturn(friendships);
    when(friendshipRepository.findPendingByPlayers(any(), any())).thenReturn(Optional.empty());
    for (final var friendship : friendships) {
      final var friend = friendship.counterpartOf(viewer);
      when(friendshipRepository.findAcceptedByPlayer(friend)).thenReturn(List.of(friendship));
      when(friendshipRepository.findAcceptedByPlayers(viewer, friend)).thenReturn(
          Optional.of(friendship));
      when(friendshipRepository.findAcceptedByPlayers(friend, viewer)).thenReturn(
          Optional.of(friendship));
    }

    final var userRepository = mock(UserQueryRepository.class);
    when(userRepository.findUsernamesByIds(anySet())).thenAnswer(invocation -> {
      final java.util.Set<PlayerId> ids = invocation.getArgument(0);
      return usernames.entrySet().stream().filter(entry -> ids.contains(entry.getKey()))
          .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    });

    return new FriendAvailabilityResolver(friendshipRepository, new MatchRepo(matchResolver),
        userRepository, availabilityChecker, onlinePresencePort, resourceInvitationQueryRepository);
  }

  private static ResourceInvitation pendingInvitation(final PlayerId sender,
      final PlayerId recipient) {

    return ResourceInvitation.create(sender, recipient, ResourceInvitationTargetType.MATCH,
        MatchId.generate().value().toString(), Instant.now(), Duration.ofMinutes(5));
  }

  private static PlayerAvailabilityChecker availabilityChecker(final boolean unfinishedMatch,
      final boolean quickQueued) {

    return availabilityChecker(unfinishedMatch, quickQueued, false, false, false);
  }

  private static PlayerAvailabilityChecker availabilityChecker(final boolean unfinishedMatch,
      final boolean quickQueued, final boolean openRematch) {

    return availabilityChecker(unfinishedMatch, quickQueued, openRematch, false, false);
  }

  private static PlayerAvailabilityChecker availabilityChecker(final boolean unfinishedMatch,
      final boolean quickQueued, final boolean openRematch, final boolean leagueWaiting,
      final boolean cupWaiting) {

    final var matchRepo = mock(MatchQueryRepository.class);
    when(matchRepo.hasUnfinishedMatch(any())).thenReturn(unfinishedMatch);
    when(matchRepo.hasActiveMatch(any())).thenReturn(unfinishedMatch);
    final var leagueRepo = mock(LeagueQueryRepository.class);
    when(leagueRepo.findInProgressByPlayer(any())).thenReturn(Optional.empty());
    when(leagueRepo.findWaitingByPlayer(any())).thenReturn(
        leagueWaiting ? Optional.of(mock(League.class)) : Optional.empty());
    final var cupRepo = mock(CupQueryRepository.class);
    when(cupRepo.findInProgressByPlayer(any())).thenReturn(Optional.empty());
    when(cupRepo.findWaitingByPlayer(any())).thenReturn(
        cupWaiting ? Optional.of(mock(Cup.class)) : Optional.empty());
    final var botRegistry = mock(BotRegistry.class);
    final var rematchRepo = mock(RematchSessionRepository.class);
    when(rematchRepo.findOpenByPlayer(any())).thenReturn(
        openRematch ? Optional.of(mock(RematchSession.class)) : Optional.empty());
    final var quickMatchQueue = mock(QuickMatchQueuePort.class);
    when(quickMatchQueue.isPlayerQueued(any())).thenReturn(quickQueued);
    return new PlayerAvailabilityChecker(matchRepo, leagueRepo, cupRepo, botRegistry, rematchRepo,
        quickMatchQueue, NoOpSpectatorshipRepository.INSTANCE, new com.villo.truco.testutil.InMemoryBotVsBotMatchRegistry());
  }

  @Test
  @DisplayName("marca disponible cuando no hay ningun bloqueo")
  void marksAvailableWithoutBlock() {

    final var viewer = PlayerId.generate();
    final var friend = PlayerId.generate();
    final var resolver = resolver(viewer, List.of(acceptedFriendship(viewer, friend)),
        Map.of(friend, "martina"), player -> Optional.empty(), availabilityChecker(false, false),
        player -> false);

    final var state = resolver.resolveState(viewer);

    assertThat(state.friends()).singleElement().satisfies(friendState -> {
      assertThat(friendState.availability()).isEqualTo(FriendAvailabilityStatus.AVAILABLE);
      assertThat(friendState.busyReason()).isNull();
    });
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
  @DisplayName("marca ocupado por revancha abierta")
  void marksBusyOpenRematch() {

    final var viewer = PlayerId.generate();
    final var friend = PlayerId.generate();
    final var resolver = resolver(viewer, List.of(acceptedFriendship(viewer, friend)),
        Map.of(friend, "martina"), player -> Optional.empty(),
        availabilityChecker(false, false, true), player -> false);

    assertThat(resolver.resolveState(viewer).friends()).singleElement().satisfies(
        friendState -> assertThat(friendState.busyReason()).isEqualTo(
            FriendBusyReason.OPEN_REMATCH));
  }

  @Test
  @DisplayName("marca ocupado por liga en espera")
  void marksBusyInLeague() {

    final var viewer = PlayerId.generate();
    final var friend = PlayerId.generate();
    final var resolver = resolver(viewer, List.of(acceptedFriendship(viewer, friend)),
        Map.of(friend, "martina"), player -> Optional.empty(),
        availabilityChecker(false, false, false, true, false), player -> false);

    assertThat(resolver.resolveState(viewer).friends()).singleElement().satisfies(friendState -> {
      assertThat(friendState.availability()).isEqualTo(FriendAvailabilityStatus.BUSY);
      assertThat(friendState.busyReason()).isEqualTo(FriendBusyReason.IN_LEAGUE);
    });
  }

  @Test
  @DisplayName("marca ocupado por copa en espera")
  void marksBusyInCup() {

    final var viewer = PlayerId.generate();
    final var friend = PlayerId.generate();
    final var resolver = resolver(viewer, List.of(acceptedFriendship(viewer, friend)),
        Map.of(friend, "martina"), player -> Optional.empty(),
        availabilityChecker(false, false, false, false, true), player -> false);

    assertThat(resolver.resolveState(viewer).friends()).singleElement().satisfies(friendState -> {
      assertThat(friendState.availability()).isEqualTo(FriendAvailabilityStatus.BUSY);
      assertThat(friendState.busyReason()).isEqualTo(FriendBusyReason.IN_CUP);
    });
  }

  @Test
  @DisplayName("marca ocupado por invitacion pendiente que bloquea entre las partes")
  void marksBusyByBlockingPendingInvitation() {

    final var viewer = PlayerId.generate();
    final var friend = PlayerId.generate();
    final var invitationRepository = mock(ResourceInvitationQueryRepository.class);
    when(invitationRepository.findPendingSentBy(viewer)).thenReturn(
        List.of(pendingInvitation(viewer, friend)));
    when(invitationRepository.findPendingReceivedBy(viewer)).thenReturn(List.of());
    final var resolver = resolver(viewer, List.of(acceptedFriendship(viewer, friend)),
        Map.of(friend, "martina"), player -> Optional.empty(), availabilityChecker(false, false),
        player -> false, invitationRepository);

    assertThat(resolver.resolveState(viewer).friends()).singleElement().satisfies(friendState -> {
      assertThat(friendState.availability()).isEqualTo(FriendAvailabilityStatus.BUSY);
      assertThat(friendState.busyReason()).isEqualTo(FriendBusyReason.PENDING_INVITATION);
    });
  }

  @Test
  @DisplayName("no marca ocupado por invitacion pendiente con otro amigo (no bloqueante)")
  void doesNotMarkBusyByPendingInvitationWithOtherFriend() {

    final var viewer = PlayerId.generate();
    final var friend = PlayerId.generate();
    final var unrelated = PlayerId.generate();
    final var invitationRepository = mock(ResourceInvitationQueryRepository.class);
    when(invitationRepository.findPendingSentBy(viewer)).thenReturn(
        List.of(pendingInvitation(viewer, unrelated)));
    when(invitationRepository.findPendingReceivedBy(viewer)).thenReturn(List.of());
    final var resolver = resolver(viewer, List.of(acceptedFriendship(viewer, friend)),
        Map.of(friend, "martina"), player -> Optional.empty(), availabilityChecker(false, false),
        player -> false, invitationRepository);

    assertThat(resolver.resolveState(viewer).friends()).singleElement().satisfies(friendState -> {
      assertThat(friendState.availability()).isEqualTo(FriendAvailabilityStatus.AVAILABLE);
      assertThat(friendState.busyReason()).isNull();
    });
  }

  @Test
  @DisplayName("un bloqueo duro tiene prioridad sobre una invitacion pendiente")
  void hardBlockTakesPriorityOverPendingInvitation() {

    final var viewer = PlayerId.generate();
    final var friend = PlayerId.generate();
    final var invitationRepository = mock(ResourceInvitationQueryRepository.class);
    when(invitationRepository.findPendingSentBy(viewer)).thenReturn(
        List.of(pendingInvitation(viewer, friend)));
    when(invitationRepository.findPendingReceivedBy(viewer)).thenReturn(List.of());
    final var resolver = resolver(viewer, List.of(acceptedFriendship(viewer, friend)),
        Map.of(friend, "martina"), player -> Optional.empty(), availabilityChecker(true, false),
        player -> false, invitationRepository);

    assertThat(resolver.resolveState(viewer).friends()).singleElement().satisfies(
        friendState -> assertThat(friendState.busyReason()).isEqualTo(FriendBusyReason.IN_MATCH));
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
  @DisplayName("solo expone disponibilidad de amistades aceptadas vigentes")
  void onlyExposesAcceptedFriends() {

    final var viewer = PlayerId.generate();
    final var resolver = resolver(viewer, List.of(), Map.of(), player -> Optional.empty(),
        availabilityChecker(false, false), player -> true);

    assertThat(resolver.resolveState(viewer).friends()).isEmpty();
  }

  @Test
  @DisplayName("delta de pareja se omite cuando ya no hay amistad aceptada vigente")
  void pairDeltaEmptyWithoutAcceptedFriendship() {

    final var viewer = PlayerId.generate();
    final var friend = PlayerId.generate();
    final var resolver = resolver(viewer, List.of(), Map.of(friend, "martina"),
        player -> Optional.empty(), availabilityChecker(false, false), player -> false);

    assertThat(resolver.resolveFriendDeltaFor(viewer, friend)).isEmpty();
  }

  @Test
  @DisplayName("delta de pareja resuelve la disponibilidad del amigo cuando la amistad vige")
  void pairDeltaResolvesWhenAcceptedFriendship() {

    final var viewer = PlayerId.generate();
    final var friend = PlayerId.generate();
    final var resolver = resolver(viewer, List.of(acceptedFriendship(viewer, friend)),
        Map.of(friend, "martina"), player -> Optional.empty(), availabilityChecker(false, false),
        player -> false);

    assertThat(resolver.resolveFriendDeltaFor(viewer, friend)).hasValueSatisfying(friendState -> {
      assertThat(friendState.friendUsername()).isEqualTo("martina");
      assertThat(friendState.availability()).isEqualTo(FriendAvailabilityStatus.AVAILABLE);
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

  @Test
  @DisplayName("delta por presencia notifica a los amigos del jugador con su snapshot online")
  void presenceDeltaTargetsFriendsWithOnlineSnapshot() {

    final var viewer = PlayerId.generate();
    final var friend = PlayerId.generate();
    final var resolver = resolver(viewer, List.of(acceptedFriendship(viewer, friend)),
        Map.of(friend, "martina"), player -> Optional.empty(), availabilityChecker(false, false),
        player -> true);

    final var changes = resolver.resolveAvailabilityChangesForPlayer(friend);

    assertThat(changes).containsOnlyKeys(viewer);
    assertThat(changes.get(viewer)).satisfies(friendState -> {
      assertThat(friendState.friendUsername()).isEqualTo("martina");
      assertThat(friendState.online()).isTrue();
      assertThat(friendState.availability()).isEqualTo(FriendAvailabilityStatus.AVAILABLE);
    });
  }

  @Test
  @DisplayName("delta por presencia sin amigos no produce destinatarios")
  void presenceDeltaWithoutFriendsIsEmpty() {

    final var player = PlayerId.generate();
    final var resolver = resolver(player, List.of(), Map.of(), playerId -> Optional.empty(),
        availabilityChecker(false, false), playerId -> true);

    assertThat(resolver.resolveAvailabilityChangesForPlayer(player)).isEmpty();
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
