package com.villo.truco.social.domain.model.friendship.valueobjects;

import com.villo.truco.domain.shared.exceptions.InvalidIdException;
import java.util.UUID;

public record FriendshipId(UUID value) {

  public static FriendshipId of(final String value) {

    try {
      return new FriendshipId(UUID.fromString(value));
    } catch (final IllegalArgumentException ex) {
      throw new InvalidIdException(value);
    }
  }

  public static FriendshipId generate() {

    return new FriendshipId(UUID.randomUUID());
  }

}
