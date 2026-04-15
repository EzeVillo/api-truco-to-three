package com.villo.truco.social.application.exceptions;

import com.villo.truco.application.exceptions.ApplicationException;
import com.villo.truco.application.exceptions.ApplicationStatus;
import com.villo.truco.social.domain.model.friendship.valueobjects.FriendshipId;

public final class FriendshipNotFoundException extends ApplicationException {

  public FriendshipNotFoundException(final FriendshipId friendshipId) {

    super(ApplicationStatus.NOT_FOUND,
        "Friendship not found for id: " + friendshipId.value().toString());
  }

  private FriendshipNotFoundException(final String message) {

    super(ApplicationStatus.NOT_FOUND, "Friendship not found for " + message);
  }

  public static FriendshipNotFoundException pendingRequestFromUsername(final String username) {

    return new FriendshipNotFoundException("pending request from username: " + username);
  }

  public static FriendshipNotFoundException pendingRequestToUsername(final String username) {

    return new FriendshipNotFoundException("pending request to username: " + username);
  }

  public static FriendshipNotFoundException acceptedFriendshipWithUsername(final String username) {

    return new FriendshipNotFoundException("accepted friendship with username: " + username);
  }

}
