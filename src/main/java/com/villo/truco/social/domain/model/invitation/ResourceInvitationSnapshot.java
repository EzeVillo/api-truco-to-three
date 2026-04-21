package com.villo.truco.social.domain.model.invitation;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationId;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationStatus;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationTargetType;
import java.time.Instant;

public record ResourceInvitationSnapshot(ResourceInvitationId id, PlayerId senderId,
                                         PlayerId recipientId,
                                         ResourceInvitationTargetType targetType, String targetId,
                                         ResourceInvitationStatus status, Instant expiresAt) {

}
