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
import com.villo.truco.social.domain.ports.FriendshipQueryRepository;
import com.villo.truco.social.domain.ports.ResourceInvitationQueryRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
      case SPECTATING -> FriendBusyReason.SPECTATING;
      case IN_LEAGUE -> FriendBusyReason.IN_LEAGUE;
      case IN_CUP -> FriendBusyReason.IN_CUP;
    };
  }

  private static SpectatableMatchRefDTO toSpectatableMatch(final Match match) {

    return new SpectatableMatchRefDTO(match.getId().value().toString(), match.getStatus().name());
  }

  public FriendAvailabilityStateDTO resolveState(final PlayerId viewerId) {

    final var friendships = this.friendshipQueryRepository.findAcceptedByPlayer(viewerId);
    final var friendIds = friendships.stream().map(friendship -> friendship.counterpartOf(viewerId))
        .toList();
    final var usernames = this.userQueryRepository.findUsernamesByIds(Set.copyOf(friendIds));
    final var friends = new ArrayList<FriendAvailabilityDTO>();

    for (final var friendId : friendIds) {
      friends.add(this.resolveFriend(viewerId, friendId, usernames.get(friendId)));
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

    final var busyReason = this.resolveBusyReason(viewerId, friendId);
    final var availability =
        busyReason != null ? FriendAvailabilityStatus.BUSY : FriendAvailabilityStatus.AVAILABLE;

    return new FriendAvailabilityDTO(friendUsername,
        this.friendOnlinePresencePort.isOnline(friendId), availability, busyReason,
        this.findSpectatableMatch(friendId));
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

  private FriendBusyReason resolveBusyReason(final PlayerId viewerId, final PlayerId friendId) {

    final var hardBlock = this.playerAvailabilityChecker.findBlockingReason(friendId);
    if (hardBlock.isPresent()) {
      return toBusyReason(hardBlock.get());
    }

    if (this.hasBlockingPendingInvitation(viewerId, friendId)) {
      return FriendBusyReason.PENDING_INVITATION;
    }

    if (this.friendshipQueryRepository.findPendingByPlayers(viewerId, friendId).isPresent()) {
      return FriendBusyReason.PENDING_FRIEND_REQUEST;
    }

    return null;
  }

  private boolean hasBlockingPendingInvitation(final PlayerId viewerId, final PlayerId friendId) {

    return this.resourceInvitationQueryRepository.findPendingSentBy(viewerId).stream()
        .anyMatch(invitation -> invitation.getRecipientId().equals(friendId))
        || this.resourceInvitationQueryRepository.findPendingReceivedBy(viewerId).stream()
        .anyMatch(invitation -> invitation.getSenderId().equals(friendId));
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

    final var changes = new LinkedHashMap<PlayerId, FriendAvailabilityDTO>();
    for (final var recipient : recipients) {
      changes.put(recipient, this.resolveFriend(recipient, activePlayer, activeUsername));
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

    final var changes = new LinkedHashMap<PlayerId, FriendAvailabilityDTO>();
    for (final var recipient : recipients) {
      changes.put(recipient, this.resolveFriend(recipient, player, username));
    }
    return changes;
  }

  private SpectatableMatchRefDTO findSpectatableMatch(final PlayerId friendId) {

    return this.matchQueryRepository.findUnfinishedByPlayer(friendId)
        .filter(match -> match.getStatus() == MatchStatus.IN_PROGRESS)
        .map(FriendAvailabilityResolver::toSpectatableMatch).orElse(null);
  }

}
