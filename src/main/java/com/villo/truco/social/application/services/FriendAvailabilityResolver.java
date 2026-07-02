package com.villo.truco.social.application.services;

import com.villo.truco.application.usecases.commands.PlayerAvailabilityChecker;
import com.villo.truco.application.usecases.commands.PlayerAvailabilityChecker.BlockingReason;
import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.dto.FriendAvailabilityDTO;
import com.villo.truco.social.application.dto.FriendAvailabilityStateDTO;
import com.villo.truco.social.application.dto.FriendAvailabilityStatus;
import com.villo.truco.social.application.dto.FriendBusyReason;
import com.villo.truco.social.application.dto.SpectatableMatchRefDTO;
import com.villo.truco.social.application.ports.out.FriendOnlinePresencePort;
import com.villo.truco.social.domain.model.invitation.ResourceInvitation;
import com.villo.truco.social.domain.ports.FriendshipQueryRepository;
import com.villo.truco.social.domain.ports.ResourceInvitationQueryRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class FriendAvailabilityResolver {

  private final FriendshipQueryRepository friendshipQueryRepository;
  private final MatchQueryRepository matchQueryRepository;
  private final UserQueryRepository userQueryRepository;
  private final PlayerAvailabilityChecker playerAvailabilityChecker;
  private final FriendOnlinePresencePort friendOnlinePresencePort;
  private final ResourceInvitationQueryRepository resourceInvitationQueryRepository;

  public FriendAvailabilityResolver(final FriendshipQueryRepository friendshipQueryRepository,
      final MatchQueryRepository matchQueryRepository,
      final UserQueryRepository userQueryRepository,
      final PlayerAvailabilityChecker playerAvailabilityChecker,
      final FriendOnlinePresencePort friendOnlinePresencePort,
      final ResourceInvitationQueryRepository resourceInvitationQueryRepository) {

    this.friendshipQueryRepository = Objects.requireNonNull(friendshipQueryRepository);
    this.matchQueryRepository = Objects.requireNonNull(matchQueryRepository);
    this.userQueryRepository = Objects.requireNonNull(userQueryRepository);
    this.playerAvailabilityChecker = Objects.requireNonNull(playerAvailabilityChecker);
    this.friendOnlinePresencePort = Objects.requireNonNull(friendOnlinePresencePort);
    this.resourceInvitationQueryRepository = Objects.requireNonNull(
        resourceInvitationQueryRepository);
  }

  private static FriendBusyReason toBusyReason(final BlockingReason blockingReason) {

    return switch (blockingReason) {
      case IN_MATCH -> FriendBusyReason.IN_MATCH;
      case OPEN_REMATCH -> FriendBusyReason.OPEN_REMATCH;
      case IN_QUICK_QUEUE -> FriendBusyReason.IN_QUICK_QUEUE;
      case SPECTATING, OWNS_BOT_MATCH -> FriendBusyReason.SPECTATING;
      case IN_LEAGUE -> FriendBusyReason.IN_LEAGUE;
      case IN_CUP -> FriendBusyReason.IN_CUP;
    };
  }

  private static SpectatableMatchRefDTO toSpectatableMatch(final Match match) {

    return new SpectatableMatchRefDTO(match.getId().value().toString(), match.getStatus().name());
  }

  private static SpectatableMatchRefDTO toSpectatableMatchIfInProgress(final Match match) {

    if (match == null || match.getStatus() != MatchStatus.IN_PROGRESS) {
      return null;
    }
    return toSpectatableMatch(match);
  }

  private static FriendAvailabilityStatus availabilityOf(final FriendBusyReason busyReason) {

    return busyReason != null ? FriendAvailabilityStatus.BUSY : FriendAvailabilityStatus.AVAILABLE;
  }

  private static boolean hasBlockingPendingInvitation(final PlayerId friendId,
      final ViewerPendingInvitations pendingInvitations) {

    return pendingInvitations.sent().stream()
        .anyMatch(invitation -> invitation.getRecipientId().equals(friendId))
        || pendingInvitations.received().stream()
        .anyMatch(invitation -> invitation.getSenderId().equals(friendId));
  }

  public FriendAvailabilityStateDTO resolveState(final PlayerId viewerId) {

    final var friendships = this.friendshipQueryRepository.findAcceptedByPlayer(viewerId);
    final var friendIds = friendships.stream().map(friendship -> friendship.counterpartOf(viewerId))
        .toList();
    final var friendIdSet = new LinkedHashSet<>(friendIds);
    final var usernames = this.userQueryRepository.findUsernamesByIds(friendIdSet);
    final var pendingInvitations = this.loadPendingInvitations(viewerId);
    final var pendingFriendRequests = this.loadPendingFriendRequestCounterparts(viewerId);
    final var blockingReasons = this.playerAvailabilityChecker.findBlockingReasonsFor(friendIdSet);
    final var unfinishedMatches = this.matchQueryRepository.findUnfinishedByPlayers(friendIdSet);
    final var friends = new ArrayList<FriendAvailabilityDTO>();

    for (final var friendId : friendIds) {
      final var busyReason = this.resolveBusyReasonFromBatch(friendId, blockingReasons,
          pendingInvitations, pendingFriendRequests);
      friends.add(new FriendAvailabilityDTO(usernames.get(friendId),
          this.friendOnlinePresencePort.isOnline(friendId), availabilityOf(busyReason), busyReason,
          toSpectatableMatchIfInProgress(unfinishedMatches.get(friendId))));
    }

    return new FriendAvailabilityStateDTO(friends);
  }

  /**
   * Resuelve la disponibilidad de {@code friendId} desde la perspectiva de {@code viewerId}. El
   * motivo de ocupacion prioriza los bloqueos duros del jugador (match, revancha, cola, liga, copa)
   * y, solo si no existe ninguno, considera los pendientes que realmente bloquean una invitacion
   * entre el viewer y el amigo (invitacion de recurso pendiente o solicitud de amistad pendiente).
   */
  public FriendAvailabilityDTO resolveFriend(final PlayerId viewerId, final PlayerId friendId,
      final String friendUsername) {

    return this.resolveFriendForViewer(viewerId, friendId, friendUsername,
        this.resolveHardBusyReason(friendId), this.friendOnlinePresencePort.isOnline(friendId),
        this.findSpectatableMatch(friendId), this.loadPendingInvitations(viewerId));
  }

  private FriendAvailabilityDTO resolveFriendForViewer(final PlayerId viewerId,
      final PlayerId friendId, final String friendUsername, final FriendBusyReason hardBusyReason,
      final boolean online, final SpectatableMatchRefDTO spectatableMatch,
      final ViewerPendingInvitations pendingInvitations) {

    final var busyReason = this.resolveBusyReasonLive(viewerId, friendId, hardBusyReason,
        pendingInvitations);
    return new FriendAvailabilityDTO(friendUsername, online, availabilityOf(busyReason), busyReason,
        spectatableMatch);
  }

  private FriendBusyReason resolveHardBusyReason(final PlayerId friendId) {

    return this.playerAvailabilityChecker.findBlockingReason(friendId)
        .map(FriendAvailabilityResolver::toBusyReason).orElse(null);
  }

  /**
   * Resuelve un delta de disponibilidad de {@code friendId} hacia {@code viewerId}. Devuelve vacio
   * cuando ya no existe una amistad aceptada vigente entre ambos, preservando la privacidad social
   * (FR-010/FR-011).
   */
  public Optional<FriendAvailabilityDTO> resolveFriendDeltaFor(final PlayerId viewerId,
      final PlayerId friendId) {

    if (this.friendshipQueryRepository.findAcceptedByPlayers(viewerId, friendId).isEmpty()) {
      return Optional.empty();
    }

    final var friendUsername = this.userQueryRepository.findUsernamesByIds(Set.of(friendId))
        .get(friendId);
    return Optional.of(this.resolveFriend(viewerId, friendId, friendUsername));
  }

  private FriendBusyReason resolveBusyReasonFromBatch(final PlayerId friendId,
      final Map<PlayerId, BlockingReason> blockingReasons,
      final ViewerPendingInvitations pendingInvitations,
      final Set<PlayerId> pendingFriendRequests) {

    final var hardBlock = blockingReasons.get(friendId);
    if (hardBlock != null) {
      return toBusyReason(hardBlock);
    }

    if (hasBlockingPendingInvitation(friendId, pendingInvitations)) {
      return FriendBusyReason.PENDING_INVITATION;
    }

    if (pendingFriendRequests.contains(friendId)) {
      return FriendBusyReason.PENDING_FRIEND_REQUEST;
    }

    return null;
  }

  private FriendBusyReason resolveBusyReasonLive(final PlayerId viewerId, final PlayerId friendId,
      final FriendBusyReason hardBusyReason, final ViewerPendingInvitations pendingInvitations) {

    if (hardBusyReason != null) {
      return hardBusyReason;
    }

    if (hasBlockingPendingInvitation(friendId, pendingInvitations)) {
      return FriendBusyReason.PENDING_INVITATION;
    }

    if (this.friendshipQueryRepository.findPendingByPlayers(viewerId, friendId).isPresent()) {
      return FriendBusyReason.PENDING_FRIEND_REQUEST;
    }

    return null;
  }

  private Set<PlayerId> loadPendingFriendRequestCounterparts(final PlayerId viewerId) {

    final var counterparts = new HashSet<PlayerId>();
    for (final var friendship : this.friendshipQueryRepository.findPendingSentBy(viewerId)) {
      counterparts.add(friendship.counterpartOf(viewerId));
    }
    for (final var friendship : this.friendshipQueryRepository.findPendingReceivedBy(viewerId)) {
      counterparts.add(friendship.counterpartOf(viewerId));
    }
    return counterparts;
  }

  private ViewerPendingInvitations loadPendingInvitations(final PlayerId viewerId) {

    return new ViewerPendingInvitations(
        this.resourceInvitationQueryRepository.findPendingSentBy(viewerId),
        this.resourceInvitationQueryRepository.findPendingReceivedBy(viewerId));
  }

  public Map<PlayerId, FriendAvailabilityDTO> resolveAvailabilityChangesByRecipient(
      final PlayerId playerOne, final PlayerId playerTwo, final PlayerId activePlayer) {

    final var recipients = this.friendshipQueryRepository.findAcceptedByPlayer(activePlayer)
        .stream().map(friendship -> friendship.counterpartOf(activePlayer))
        .filter(friendId -> !friendId.equals(playerOne))
        .filter(friendId -> !friendId.equals(playerTwo)).toList();
    if (recipients.isEmpty()) {
      return Map.of();
    }

    final var activeUsername = this.userQueryRepository.findUsernamesByIds(Set.of(activePlayer))
        .get(activePlayer);
    return getPlayerIdFriendAvailabilityDTOMap(activePlayer, recipients, activeUsername);
  }

  private Map<PlayerId, FriendAvailabilityDTO> getPlayerIdFriendAvailabilityDTOMap(
      final PlayerId activePlayer, final List<PlayerId> recipients, final String activeUsername) {

    final var hardBusyReason = this.resolveHardBusyReason(activePlayer);
    final var online = this.friendOnlinePresencePort.isOnline(activePlayer);
    final var spectatableMatch = this.findSpectatableMatch(activePlayer);

    final var changes = new LinkedHashMap<PlayerId, FriendAvailabilityDTO>();
    for (final var recipient : recipients) {
      changes.put(recipient,
          this.resolveFriendForViewer(recipient, activePlayer, activeUsername, hardBusyReason,
              online, spectatableMatch, this.loadPendingInvitations(recipient)));
    }
    return changes;
  }

  public Map<PlayerId, FriendAvailabilityDTO> resolveAvailabilityChangesForPlayer(
      final PlayerId player) {

    final var recipients = this.friendshipQueryRepository.findAcceptedByPlayer(player).stream()
        .map(friendship -> friendship.counterpartOf(player)).toList();
    if (recipients.isEmpty()) {
      return Map.of();
    }

    final var username = this.userQueryRepository.findUsernamesByIds(Set.of(player)).get(player);
    return getPlayerIdFriendAvailabilityDTOMap(player, recipients, username);
  }

  private SpectatableMatchRefDTO findSpectatableMatch(final PlayerId friendId) {

    return this.matchQueryRepository.findUnfinishedByPlayer(friendId)
        .filter(match -> match.getStatus() == MatchStatus.IN_PROGRESS)
        .map(FriendAvailabilityResolver::toSpectatableMatch).orElse(null);
  }

  private record ViewerPendingInvitations(List<ResourceInvitation> sent,
                                          List<ResourceInvitation> received) {

  }

}
