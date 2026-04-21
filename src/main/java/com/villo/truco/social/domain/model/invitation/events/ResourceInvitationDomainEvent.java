package com.villo.truco.social.domain.model.invitation.events;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.friendship.events.SocialDomainEvent;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationId;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationStatus;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationTargetType;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public abstract class ResourceInvitationDomainEvent extends SocialDomainEvent {

  private final ResourceInvitationId invitationId;
  private final PlayerId senderId;
  private final PlayerId recipientId;
  private final ResourceInvitationTargetType targetType;
  private final String targetId;
  private final ResourceInvitationStatus status;
  private final Instant expiresAt;

  protected ResourceInvitationDomainEvent(final String eventType,
      final ResourceInvitationId invitationId, final PlayerId senderId, final PlayerId recipientId,
      final ResourceInvitationTargetType targetType, final String targetId,
      final ResourceInvitationStatus status, final Instant expiresAt,
      final List<PlayerId> recipients) {

    super(eventType, recipients);
    this.invitationId = Objects.requireNonNull(invitationId);
    this.senderId = Objects.requireNonNull(senderId);
    this.recipientId = Objects.requireNonNull(recipientId);
    this.targetType = Objects.requireNonNull(targetType);
    this.targetId = Objects.requireNonNull(targetId);
    this.status = Objects.requireNonNull(status);
    this.expiresAt = Objects.requireNonNull(expiresAt);
  }

  public ResourceInvitationId getInvitationId() {

    return this.invitationId;
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

}
