package com.villo.truco.social.domain.model.friendship.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class CannotFriendYourselfException extends DomainException {

  public CannotFriendYourselfException() {

    super("Cannot create a friendship with yourself");
  }

}
