package com.villo.truco.social.domain.model.friendship.events;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.friendship.valueobjects.FriendshipId;
import com.villo.truco.social.domain.model.friendship.valueobjects.FriendshipStatus;
import java.util.List;
import java.util.Objects;

public abstract class FriendshipDomainEvent extends SocialDomainEvent {

  private final FriendshipId friendshipId;
  private final PlayerId requesterId;
  private final PlayerId addresseeId;
  private final FriendshipStatus status;

  protected FriendshipDomainEvent(final String eventType, final FriendshipId friendshipId,
      final PlayerId requesterId, final PlayerId addresseeId, final FriendshipStatus status,
      final List<PlayerId> recipients) {

    super(eventType, recipients);
    this.friendshipId = Objects.requireNonNull(friendshipId);
    this.requesterId = Objects.requireNonNull(requesterId);
    this.addresseeId = Objects.requireNonNull(addresseeId);
    this.status = Objects.requireNonNull(status);
  }

  public FriendshipId getFriendshipId() {

    return this.friendshipId;
  }

  public PlayerId getRequesterId() {

    return this.requesterId;
  }

  public PlayerId getAddresseeId() {

    return this.addresseeId;
  }

  public FriendshipStatus getStatus() {

    return this.status;
  }

}
