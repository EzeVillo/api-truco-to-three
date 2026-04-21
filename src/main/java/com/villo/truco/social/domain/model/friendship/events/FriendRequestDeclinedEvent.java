package com.villo.truco.social.domain.model.friendship.events;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.friendship.valueobjects.FriendshipId;
import com.villo.truco.social.domain.model.friendship.valueobjects.FriendshipStatus;
import java.util.List;

public final class FriendRequestDeclinedEvent extends FriendshipDomainEvent {

  public FriendRequestDeclinedEvent(final FriendshipId friendshipId, final PlayerId requesterId,
      final PlayerId addresseeId) {

    super("FRIEND_REQUEST_DECLINED", friendshipId, requesterId, addresseeId,
        FriendshipStatus.DECLINED, List.of(requesterId));
  }

}
