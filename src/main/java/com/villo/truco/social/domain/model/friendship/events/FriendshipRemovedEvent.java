package com.villo.truco.social.domain.model.friendship.events;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.friendship.valueobjects.FriendshipId;
import com.villo.truco.social.domain.model.friendship.valueobjects.FriendshipStatus;
import java.util.List;

public final class FriendshipRemovedEvent extends FriendshipDomainEvent {

  private final PlayerId removedById;

  public FriendshipRemovedEvent(final FriendshipId friendshipId, final PlayerId requesterId,
      final PlayerId addresseeId, final PlayerId removedById) {

    super("FRIENDSHIP_REMOVED", friendshipId, requesterId, addresseeId, FriendshipStatus.REMOVED,
        List.of(requesterId, addresseeId));
    this.removedById = removedById;
  }

  public PlayerId getRemovedById() {

    return this.removedById;
  }

}
