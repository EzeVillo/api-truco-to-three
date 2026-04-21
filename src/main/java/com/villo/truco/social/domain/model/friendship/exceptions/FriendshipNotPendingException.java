package com.villo.truco.social.domain.model.friendship.exceptions;

import com.villo.truco.domain.shared.DomainException;
import com.villo.truco.social.domain.model.friendship.valueobjects.FriendshipStatus;

public final class FriendshipNotPendingException extends DomainException {

  public FriendshipNotPendingException(final FriendshipStatus status) {

    super("Friendship must be pending but was " + status.name());
  }

}
