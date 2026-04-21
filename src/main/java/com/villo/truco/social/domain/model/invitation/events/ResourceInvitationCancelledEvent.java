package com.villo.truco.social.domain.model.invitation.events;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationId;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationStatus;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationTargetType;
import java.time.Instant;
import java.util.List;

public final class ResourceInvitationCancelledEvent extends ResourceInvitationDomainEvent {

  public ResourceInvitationCancelledEvent(final ResourceInvitationId invitationId,
      final PlayerId senderId, final PlayerId recipientId,
      final ResourceInvitationTargetType targetType, final String targetId,
      final Instant expiresAt) {

    super("RESOURCE_INVITATION_CANCELLED", invitationId, senderId, recipientId, targetType,
        targetId, ResourceInvitationStatus.CANCELLED, expiresAt, List.of(recipientId));
  }

}
