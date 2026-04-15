package com.villo.truco.social.domain.model.friendship.exceptions;

import com.villo.truco.domain.shared.DomainException;
import com.villo.truco.social.domain.model.friendship.valueobjects.FriendshipStatus;

public final class FriendshipNotAcceptedException extends DomainException {

  public FriendshipNotAcceptedException(final FriendshipStatus status) {

    super("Friendship must be accepted but was " + status.name());
  }

}
