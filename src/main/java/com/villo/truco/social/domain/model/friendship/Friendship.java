package com.villo.truco.social.domain.model.friendship;

import com.villo.truco.domain.shared.AggregateBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.friendship.events.FriendRequestAcceptedEvent;
import com.villo.truco.social.domain.model.friendship.events.FriendRequestCancelledEvent;
import com.villo.truco.social.domain.model.friendship.events.FriendRequestDeclinedEvent;
import com.villo.truco.social.domain.model.friendship.events.FriendRequestReceivedEvent;
import com.villo.truco.social.domain.model.friendship.events.FriendshipDomainEvent;
import com.villo.truco.social.domain.model.friendship.events.FriendshipRemovedEvent;
import com.villo.truco.social.domain.model.friendship.exceptions.CannotFriendYourselfException;
import com.villo.truco.social.domain.model.friendship.exceptions.FriendshipNotAcceptedException;
import com.villo.truco.social.domain.model.friendship.exceptions.FriendshipNotPendingException;
import com.villo.truco.social.domain.model.friendship.exceptions.OnlyAddresseeCanRespondFriendRequestException;
import com.villo.truco.social.domain.model.friendship.exceptions.OnlyRequesterCanCancelFriendRequestException;
import com.villo.truco.social.domain.model.friendship.exceptions.PlayerNotPartOfFriendshipException;
import com.villo.truco.social.domain.model.friendship.valueobjects.FriendshipId;
import com.villo.truco.social.domain.model.friendship.valueobjects.FriendshipStatus;
import java.util.List;
import java.util.Objects;

public final class Friendship extends AggregateBase<FriendshipId> {

  private final PlayerId requesterId;
  private final PlayerId addresseeId;
  private FriendshipStatus status;

  private Friendship(final FriendshipId id, final PlayerId requesterId, final PlayerId addresseeId,
      final FriendshipStatus status) {

    super(id);
    this.requesterId = Objects.requireNonNull(requesterId);
    this.addresseeId = Objects.requireNonNull(addresseeId);
    this.status = Objects.requireNonNull(status);
  }

  static Friendship reconstruct(final FriendshipId id, final PlayerId requesterId,
      final PlayerId addresseeId, final FriendshipStatus status) {

    return new Friendship(id, requesterId, addresseeId, status);
  }

  public static Friendship request(final PlayerId requesterId, final PlayerId addresseeId) {

    Objects.requireNonNull(requesterId, "Requester id cannot be null");
    Objects.requireNonNull(addresseeId, "Addressee id cannot be null");
    if (requesterId.equals(addresseeId)) {
      throw new CannotFriendYourselfException();
    }

    final var friendship = new Friendship(FriendshipId.generate(), requesterId, addresseeId,
        FriendshipStatus.PENDING);
    friendship.addDomainEvent(
        new FriendRequestReceivedEvent(friendship.getId(), requesterId, addresseeId));
    return friendship;
  }

  public void accept(final PlayerId actorId) {

    this.ensurePending();
    if (!this.addresseeId.equals(actorId)) {
      throw new OnlyAddresseeCanRespondFriendRequestException();
    }

    this.status = FriendshipStatus.ACCEPTED;
    this.addDomainEvent(
        new FriendRequestAcceptedEvent(this.id, this.requesterId, this.addresseeId));
  }

  public void decline(final PlayerId actorId) {

    this.ensurePending();
    if (!this.addresseeId.equals(actorId)) {
      throw new OnlyAddresseeCanRespondFriendRequestException();
    }

    this.status = FriendshipStatus.DECLINED;
    this.addDomainEvent(
        new FriendRequestDeclinedEvent(this.id, this.requesterId, this.addresseeId));
  }

  public void cancel(final PlayerId actorId) {

    this.ensurePending();
    if (!this.requesterId.equals(actorId)) {
      throw new OnlyRequesterCanCancelFriendRequestException();
    }

    this.status = FriendshipStatus.CANCELLED;
    this.addDomainEvent(
        new FriendRequestCancelledEvent(this.id, this.requesterId, this.addresseeId));
  }

  public void remove(final PlayerId actorId) {

    this.ensureAccepted();
    this.ensureParticipant(actorId);

    this.status = FriendshipStatus.REMOVED;
    this.addDomainEvent(
        new FriendshipRemovedEvent(this.id, this.requesterId, this.addresseeId, actorId));
  }

  public boolean involves(final PlayerId playerId) {

    return this.requesterId.equals(playerId) || this.addresseeId.equals(playerId);
  }

  public PlayerId counterpartOf(final PlayerId playerId) {

    this.ensureParticipant(playerId);
    return this.requesterId.equals(playerId) ? this.addresseeId : this.requesterId;
  }

  public boolean isAccepted() {

    return this.status == FriendshipStatus.ACCEPTED;
  }

  public FriendshipSnapshot snapshot() {

    return new FriendshipSnapshot(this.id, this.requesterId, this.addresseeId, this.status);
  }

  public List<FriendshipDomainEvent> getFriendshipDomainEvents() {

    return this.getDomainEvents().stream().map(FriendshipDomainEvent.class::cast).toList();
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

  private void ensurePending() {

    if (this.status != FriendshipStatus.PENDING) {
      throw new FriendshipNotPendingException(this.status);
    }
  }

  private void ensureAccepted() {

    if (this.status != FriendshipStatus.ACCEPTED) {
      throw new FriendshipNotAcceptedException(this.status);
    }
  }

  private void ensureParticipant(final PlayerId playerId) {

    if (!this.involves(playerId)) {
      throw new PlayerNotPartOfFriendshipException(playerId);
    }
  }

}
