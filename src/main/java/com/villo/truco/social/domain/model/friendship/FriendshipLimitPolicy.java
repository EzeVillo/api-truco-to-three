package com.villo.truco.social.domain.model.friendship;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.friendship.exceptions.FriendLimitReachedException;
import com.villo.truco.social.domain.ports.FriendshipQueryRepository;
import java.util.Objects;

public final class FriendshipLimitPolicy {

  private final FriendshipQueryRepository friendshipQueryRepository;
  private final int maxFriends;

  public FriendshipLimitPolicy(final FriendshipQueryRepository friendshipQueryRepository,
      final int maxFriends) {

    this.friendshipQueryRepository = Objects.requireNonNull(friendshipQueryRepository);
    if (maxFriends < 1) {
      throw new IllegalArgumentException("maxFriends must be at least 1, was " + maxFriends);
    }
    this.maxFriends = maxFriends;
  }

  public void ensureSelfHasRoom(final PlayerId playerId) {

    if (this.isAtLimit(playerId)) {
      throw FriendLimitReachedException.forSelf(this.maxFriends);
    }
  }

  public void ensureCounterpartHasRoom(final PlayerId playerId) {

    if (this.isAtLimit(playerId)) {
      throw FriendLimitReachedException.forCounterpart(this.maxFriends);
    }
  }

  private boolean isAtLimit(final PlayerId playerId) {

    return this.friendshipQueryRepository.countAcceptedByPlayer(playerId) >= this.maxFriends;
  }

}
