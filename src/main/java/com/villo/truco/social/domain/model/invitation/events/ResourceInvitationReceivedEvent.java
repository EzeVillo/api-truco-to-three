package com.villo.truco.social.domain.model.invitation.events;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationId;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationStatus;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationTargetType;
import java.time.Instant;
import java.util.List;

public final class ResourceInvitationReceivedEvent extends ResourceInvitationDomainEvent {

  public ResourceInvitationReceivedEvent(final ResourceInvitationId invitationId,
      final PlayerId senderId, final PlayerId recipientId,
      final ResourceInvitationTargetType targetType, final String targetId,
      final Instant expiresAt) {

    super("RESOURCE_INVITATION_RECEIVED", invitationId, senderId, recipientId, targetType, targetId,
        ResourceInvitationStatus.PENDING, expiresAt, List.of(recipientId));
  }

}
