package com.villo.truco.social.domain.model.friendship.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class FriendRequestsNotAcceptedException extends DomainException {

  public FriendRequestsNotAcceptedException() {

    super("Este jugador no recibe solicitudes de amistad");
  }

}
