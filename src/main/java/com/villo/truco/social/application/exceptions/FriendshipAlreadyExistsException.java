package com.villo.truco.social.application.exceptions;

import com.villo.truco.application.exceptions.ApplicationException;
import com.villo.truco.application.exceptions.ApplicationStatus;

public final class FriendshipAlreadyExistsException extends ApplicationException {

  public FriendshipAlreadyExistsException() {

    super(ApplicationStatus.CONFLICT, "A friendship already exists for this pair of players");
  }

}
