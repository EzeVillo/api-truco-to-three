package com.villo.truco.social.domain.model.friendship.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class OnlyAddresseeCanRespondFriendRequestException extends DomainException {

  public OnlyAddresseeCanRespondFriendRequestException() {

    super("Only the addressee can respond to the friendship request");
  }

}
