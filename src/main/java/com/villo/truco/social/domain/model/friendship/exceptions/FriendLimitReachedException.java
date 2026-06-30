package com.villo.truco.social.domain.model.friendship.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class FriendLimitReachedException extends DomainException {

  private FriendLimitReachedException(final String message) {

    super(message);
  }

  public static FriendLimitReachedException forSelf(final int maxFriends) {

    return new FriendLimitReachedException(
        "Alcanzaste el máximo de " + maxFriends + " amigos permitidos");
  }

  public static FriendLimitReachedException forCounterpart(final int maxFriends) {

    return new FriendLimitReachedException(
        "Este jugador alcanzó el máximo de " + maxFriends + " amigos permitidos");
  }

}
