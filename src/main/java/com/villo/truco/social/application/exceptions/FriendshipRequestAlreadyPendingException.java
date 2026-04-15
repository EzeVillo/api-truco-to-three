package com.villo.truco.social.application.exceptions;

import com.villo.truco.application.exceptions.ApplicationException;
import com.villo.truco.application.exceptions.ApplicationStatus;

public final class FriendshipRequestAlreadyPendingException extends ApplicationException {

  public FriendshipRequestAlreadyPendingException() {

    super(ApplicationStatus.CONFLICT,
        "A pending friendship request already exists for this pair of players");
  }

}
