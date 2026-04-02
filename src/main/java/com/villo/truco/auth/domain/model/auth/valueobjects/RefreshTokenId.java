package com.villo.truco.auth.domain.model.auth.valueobjects;

import com.villo.truco.domain.shared.exceptions.InvalidIdException;
import java.util.UUID;

public record RefreshTokenId(UUID value) {

  public static RefreshTokenId generate() {

    return new RefreshTokenId(UUID.randomUUID());
  }

  public static RefreshTokenId of(final String value) {

    try {
      return new RefreshTokenId(UUID.fromString(value));
    } catch (final IllegalArgumentException e) {
      throw new InvalidIdException(value);
    }
  }

  public UserSessionId toUserSessionId() {

    return new UserSessionId(this.value);
  }

}
