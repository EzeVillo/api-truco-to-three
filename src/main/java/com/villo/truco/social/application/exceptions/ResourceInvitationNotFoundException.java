package com.villo.truco.social.application.exceptions;

import com.villo.truco.application.exceptions.ApplicationException;
import com.villo.truco.application.exceptions.ApplicationStatus;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationId;

public final class ResourceInvitationNotFoundException extends ApplicationException {

  public ResourceInvitationNotFoundException(final ResourceInvitationId invitationId) {

    super(ApplicationStatus.NOT_FOUND,
        "Resource invitation not found for id: " + invitationId.value().toString());
  }

}
