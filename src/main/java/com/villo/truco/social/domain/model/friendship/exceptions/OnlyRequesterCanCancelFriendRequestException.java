package com.villo.truco.social.domain.model.friendship.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class OnlyRequesterCanCancelFriendRequestException extends DomainException {

  public OnlyRequesterCanCancelFriendRequestException() {

    super("Only the requester can cancel the friendship request");
  }

}
