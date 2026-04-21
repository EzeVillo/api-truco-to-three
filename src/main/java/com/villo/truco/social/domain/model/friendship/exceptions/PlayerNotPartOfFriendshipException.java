package com.villo.truco.social.domain.model.friendship.exceptions;

import com.villo.truco.domain.shared.DomainException;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public final class PlayerNotPartOfFriendshipException extends DomainException {

  public PlayerNotPartOfFriendshipException(final PlayerId playerId) {

    super("Player " + playerId.value() + " is not part of the friendship");
  }

}
