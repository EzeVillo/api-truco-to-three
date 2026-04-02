package com.villo.truco.auth.domain.model.auth.valueobjects;

import com.villo.truco.domain.shared.exceptions.InvalidIdException;
import java.util.UUID;

public record UserSessionId(UUID value) {

  public static UserSessionId generate() {

    return new UserSessionId(UUID.randomUUID());
  }

  public static UserSessionId of(final String value) {

    try {
      return new UserSessionId(UUID.fromString(value));
    } catch (final IllegalArgumentException e) {
      throw new InvalidIdException(value);
    }
  }

}
