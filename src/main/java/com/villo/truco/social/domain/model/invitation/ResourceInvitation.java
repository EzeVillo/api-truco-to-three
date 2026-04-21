package com.villo.truco.social.domain.model.invitation;

import com.villo.truco.domain.shared.AggregateBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationAcceptedEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationCancelledEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationDeclinedEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationDomainEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationExpiredEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationReceivedEvent;
import com.villo.truco.social.domain.model.invitation.exceptions.OnlyRecipientCanRespondResourceInvitationException;
import com.villo.truco.social.domain.model.invitation.exceptions.OnlySenderCanCancelResourceInvitationException;
import com.villo.truco.social.domain.model.invitation.exceptions.ResourceInvitationNotPendingException;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationId;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationStatus;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationTargetType;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class ResourceInvitation extends AggregateBase<ResourceInvitationId> {

  private final PlayerId senderId;
  private final PlayerId recipientId;
  private final ResourceInvitationTargetType targetType;
  private final String targetId;
  private final Instant expiresAt;
  private ResourceInvitationStatus status;

  private ResourceInvitation(final ResourceInvitationId id, final PlayerId senderId,
      final PlayerId recipientId, final ResourceInvitationTargetType targetType,
      final String targetId, final ResourceInvitationStatus status, final Instant expiresAt) {

    super(id);
    this.senderId = Objects.requireNonNull(senderId);
    this.recipientId = Objects.requireNonNull(recipientId);
    this.targetType = Objects.requireNonNull(targetType);
    this.targetId = Objects.requireNonNull(targetId);
    this.status = Objects.requireNonNull(status);
    this.expiresAt = Objects.requireNonNull(expiresAt);
  }

  static ResourceInvitation reconstruct(final ResourceInvitationId id, final PlayerId senderId,
      final PlayerId recipientId, final ResourceInvitationTargetType targetType,
      final String targetId, final ResourceInvitationStatus status, final Instant expiresAt) {

    return new ResourceInvitation(id, senderId, recipientId, targetType, targetId, status,
        expiresAt);
  }

  public static ResourceInvitation create(final PlayerId senderId, final PlayerId recipientId,
      final ResourceInvitationTargetType targetType, final String targetId, final Instant now,
      final Duration expirationDuration) {

    Objects.requireNonNull(senderId, "Sender id cannot be null");
    Objects.requireNonNull(recipientId, "Recipient id cannot be null");
    Objects.requireNonNull(targetType, "Target type cannot be null");
    Objects.requireNonNull(targetId, "Target id cannot be null");
    Objects.requireNonNull(now, "Now cannot be null");
    Objects.requireNonNull(expirationDuration, "Expiration duration cannot be null");

    final var expiresAt = now.plus(expirationDuration);
    final var invitation = new ResourceInvitation(ResourceInvitationId.generate(), senderId,
        recipientId, targetType, targetId, ResourceInvitationStatus.PENDING, expiresAt);
    invitation.addDomainEvent(
        new ResourceInvitationReceivedEvent(invitation.id, senderId, recipientId, targetType,
            targetId, expiresAt));
    return invitation;
  }

  public void accept(final PlayerId actorId) {

    this.ensurePending();

    if (!this.recipientId.equals(actorId)) {
      throw new OnlyRecipientCanRespondResourceInvitationException();
    }

    this.status = ResourceInvitationStatus.ACCEPTED;
    this.addDomainEvent(
        new ResourceInvitationAcceptedEvent(this.id, this.senderId, this.recipientId,
            this.targetType, this.targetId, this.expiresAt));
  }

  public void decline(final PlayerId actorId) {

    this.ensurePending();

    if (!this.recipientId.equals(actorId)) {
      throw new OnlyRecipientCanRespondResourceInvitationException();
    }

    this.status = ResourceInvitationStatus.DECLINED;
    this.addDomainEvent(
        new ResourceInvitationDeclinedEvent(this.id, this.senderId, this.recipientId,
            this.targetType, this.targetId, this.expiresAt));
  }

  public void cancel(final PlayerId actorId) {

    this.ensurePending();

    if (!this.senderId.equals(actorId)) {
      throw new OnlySenderCanCancelResourceInvitationException();
    }

    this.status = ResourceInvitationStatus.CANCELLED;
    this.addDomainEvent(
        new ResourceInvitationCancelledEvent(this.id, this.senderId, this.recipientId,
            this.targetType, this.targetId, this.expiresAt));
  }

  public boolean expireIfNeeded(final Instant now) {

    if (this.status != ResourceInvitationStatus.PENDING || now.isBefore(this.expiresAt)) {
      return false;
    }

    this.expire();
    return true;
  }

  public void expire() {

    if (this.status != ResourceInvitationStatus.PENDING) {
      return;
    }

    this.status = ResourceInvitationStatus.EXPIRED;
    this.addDomainEvent(new ResourceInvitationExpiredEvent(this.id, this.senderId, this.recipientId,
        this.targetType, this.targetId, this.expiresAt));
  }

  public ResourceInvitationSnapshot snapshot() {

    return new ResourceInvitationSnapshot(this.id, this.senderId, this.recipientId, this.targetType,
        this.targetId, this.status, this.expiresAt);
  }

  public List<ResourceInvitationDomainEvent> getResourceInvitationDomainEvents() {

    return this.getDomainEvents().stream().map(ResourceInvitationDomainEvent.class::cast).toList();
  }

  public boolean isPending() {

    return this.status == ResourceInvitationStatus.PENDING;
  }

  public PlayerId getSenderId() {

    return this.senderId;
  }

  public PlayerId getRecipientId() {

    return this.recipientId;
  }

  public ResourceInvitationTargetType getTargetType() {

    return this.targetType;
  }

  public String getTargetId() {

    return this.targetId;
  }

  public ResourceInvitationStatus getStatus() {

    return this.status;
  }

  public Instant getExpiresAt() {

    return this.expiresAt;
  }

  private void ensurePending() {

    if (this.status != ResourceInvitationStatus.PENDING) {
      throw new ResourceInvitationNotPendingException(this.status);
    }
  }

}
