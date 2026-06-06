package com.villo.truco.social.application.services;

import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.dto.FriendActivityDTO;
import com.villo.truco.social.application.dto.FriendActivityStateDTO;
import com.villo.truco.social.application.dto.SpectatableMatchRefDTO;
import com.villo.truco.social.domain.ports.FriendshipQueryRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class FriendActivityResolver {

  private final FriendshipQueryRepository friendshipQueryRepository;
  private final MatchQueryRepository matchQueryRepository;
  private final UserQueryRepository userQueryRepository;

  public FriendActivityResolver(final FriendshipQueryRepository friendshipQueryRepository,
      final MatchQueryRepository matchQueryRepository,
      final UserQueryRepository userQueryRepository) {

    this.friendshipQueryRepository = Objects.requireNonNull(friendshipQueryRepository);
    this.matchQueryRepository = Objects.requireNonNull(matchQueryRepository);
    this.userQueryRepository = Objects.requireNonNull(userQueryRepository);
  }

  private static SpectatableMatchRefDTO toSpectatableMatch(final Match match) {

    return new SpectatableMatchRefDTO(match.getId().value().toString(), match.getStatus().name());
  }

  public FriendActivityStateDTO resolveState(final PlayerId viewerId) {

    final var friendships = this.friendshipQueryRepository.findAcceptedByPlayer(viewerId);
    final var friendIds = friendships.stream().map(friendship -> friendship.counterpartOf(viewerId))
        .toList();
    final var usernames = this.userQueryRepository.findUsernamesByIds(Set.copyOf(friendIds));
    final var friends = new ArrayList<FriendActivityDTO>();

    for (final var friendId : friendIds) {
      friends.add(
          new FriendActivityDTO(usernames.get(friendId), this.findSpectatableMatch(friendId)));
    }

    return new FriendActivityStateDTO(friends);
  }

  public Map<PlayerId, FriendActivityDTO> resolveMatchActivityChangesByRecipient(
      final MatchId matchId, final PlayerId playerOne, final PlayerId playerTwo,
      final PlayerId activePlayer, final boolean spectatable) {

    final var recipients = this.friendshipQueryRepository.findAcceptedByPlayer(activePlayer)
        .stream().map(friendship -> friendship.counterpartOf(activePlayer))
        .filter(friendId -> !friendId.equals(playerOne))
        .filter(friendId -> !friendId.equals(playerTwo)).toList();
    if (recipients.isEmpty()) {
      return Map.of();
    }

    final var activeUsername = this.userQueryRepository.findUsernamesByIds(Set.of(activePlayer))
        .get(activePlayer);
    final var spectatableMatch =
        spectatable ? new SpectatableMatchRefDTO(matchId.value().toString(),
            MatchStatus.IN_PROGRESS.name()) : null;
    final var changes = new LinkedHashMap<PlayerId, FriendActivityDTO>();
    for (final var recipient : recipients) {
      changes.put(recipient, new FriendActivityDTO(activeUsername, spectatableMatch));
    }
    return changes;
  }

  private SpectatableMatchRefDTO findSpectatableMatch(final PlayerId friendId) {

    return this.matchQueryRepository.findUnfinishedByPlayer(friendId)
        .filter(match -> match.getStatus() == MatchStatus.IN_PROGRESS)
        .map(FriendActivityResolver::toSpectatableMatch).orElse(null);
  }

}
