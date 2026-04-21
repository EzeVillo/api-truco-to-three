package com.villo.truco.social.application.exceptions;

import com.villo.truco.application.exceptions.ApplicationException;
import com.villo.truco.application.exceptions.ApplicationStatus;

public final class ResourceInvitationTargetUnavailableException extends ApplicationException {

  public ResourceInvitationTargetUnavailableException() {

    super(ApplicationStatus.CONFLICT, "The resource no longer admits joins");
  }

}
