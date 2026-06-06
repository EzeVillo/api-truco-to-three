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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class FriendAvailabilityResolver {

  private final FriendshipQueryRepository friendshipQueryRepository;
  private final MatchQueryRepository matchQueryRepository;
  private final UserQueryRepository userQueryRepository;
  private final PlayerAvailabilityChecker playerAvailabilityChecker;
  private final FriendOnlinePresencePort friendOnlinePresencePort;

  public FriendAvailabilityResolver(final FriendshipQueryRepository friendshipQueryRepository,
      final MatchQueryRepository matchQueryRepository,
      final UserQueryRepository userQueryRepository,
      final PlayerAvailabilityChecker playerAvailabilityChecker,
      final FriendOnlinePresencePort friendOnlinePresencePort) {

    this.friendshipQueryRepository = Objects.requireNonNull(friendshipQueryRepository);
    this.matchQueryRepository = Objects.requireNonNull(matchQueryRepository);
    this.userQueryRepository = Objects.requireNonNull(userQueryRepository);
    this.playerAvailabilityChecker = Objects.requireNonNull(playerAvailabilityChecker);
    this.friendOnlinePresencePort = Objects.requireNonNull(friendOnlinePresencePort);
  }

  private static FriendBusyReason toBusyReason(final BlockingReason blockingReason) {

    return switch (blockingReason) {
      case IN_MATCH -> FriendBusyReason.IN_MATCH;
      case OPEN_REMATCH -> FriendBusyReason.OPEN_REMATCH;
      case IN_QUICK_QUEUE -> FriendBusyReason.IN_QUICK_QUEUE;
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
      friends.add(this.resolveFriend(friendId, usernames.get(friendId)));
    }

    return new FriendAvailabilityStateDTO(friends);
  }

  public FriendAvailabilityDTO resolveFriend(final PlayerId friendId, final String friendUsername) {

    final var blockingReason = this.playerAvailabilityChecker.findBlockingReason(friendId);
    final var availability = blockingReason.isPresent() ? FriendAvailabilityStatus.BUSY
        : FriendAvailabilityStatus.AVAILABLE;
    final var busyReason = blockingReason.map(FriendAvailabilityResolver::toBusyReason)
        .orElse(null);

    return new FriendAvailabilityDTO(friendUsername,
        this.friendOnlinePresencePort.isOnline(friendId), availability, busyReason,
        this.findSpectatableMatch(friendId));
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
    final var activePlayerAvailability = this.resolveFriend(activePlayer, activeUsername);

    final var changes = new LinkedHashMap<PlayerId, FriendAvailabilityDTO>();
    for (final var recipient : recipients) {
      changes.put(recipient, activePlayerAvailability);
    }
    return changes;
  }

  private SpectatableMatchRefDTO findSpectatableMatch(final PlayerId friendId) {

    return this.matchQueryRepository.findUnfinishedByPlayer(friendId)
        .filter(match -> match.getStatus() == MatchStatus.IN_PROGRESS)
        .map(FriendAvailabilityResolver::toSpectatableMatch).orElse(null);
  }

}
