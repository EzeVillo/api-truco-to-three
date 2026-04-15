package com.villo.truco.social.application.exceptions;

import com.villo.truco.application.exceptions.ApplicationException;
import com.villo.truco.application.exceptions.ApplicationStatus;

public final class ResourceInvitationAlreadyExistsException extends ApplicationException {

  public ResourceInvitationAlreadyExistsException() {

    super(ApplicationStatus.CONFLICT,
        "A pending resource invitation already exists for this friend and resource");
  }

}
