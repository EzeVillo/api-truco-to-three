package com.villo.truco.social.application.exceptions;

import com.villo.truco.application.exceptions.ApplicationException;
import com.villo.truco.application.exceptions.ApplicationStatus;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationTargetType;

public final class InvitableResourceNotFoundException extends ApplicationException {

  public InvitableResourceNotFoundException(final ResourceInvitationTargetType targetType,
      final String targetId) {

    super(ApplicationStatus.NOT_FOUND, targetType.name() + " not found for id: " + targetId);
  }

}
