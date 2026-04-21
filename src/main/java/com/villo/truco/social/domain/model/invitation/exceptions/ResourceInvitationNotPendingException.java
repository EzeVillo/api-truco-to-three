package com.villo.truco.social.domain.model.invitation.exceptions;

import com.villo.truco.domain.shared.DomainException;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationStatus;

public final class ResourceInvitationNotPendingException extends DomainException {

  public ResourceInvitationNotPendingException(final ResourceInvitationStatus status) {

    super("Resource invitation must be pending but was " + status.name());
  }

}
