package com.villo.truco.social.domain.ports;

import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationId;
import java.time.Instant;

public record ResourceInvitationTimeoutEntry(ResourceInvitationId invitationId, Instant expiresAt) {

}
