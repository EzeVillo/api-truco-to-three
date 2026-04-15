package com.villo.truco.social.application.exceptions;

import com.villo.truco.application.exceptions.ApplicationException;
import com.villo.truco.application.exceptions.ApplicationStatus;

public final class FriendshipRequiredException extends ApplicationException {

  public FriendshipRequiredException() {

    super(ApplicationStatus.CONFLICT, "Accepted friendship is required for this action");
  }

}
