package com.villo.truco.domain.model.rematch.valueobjects;

import com.villo.truco.domain.shared.exceptions.InvalidIdException;
import java.util.UUID;

public record RematchSessionId(UUID value) {

  public static RematchSessionId generate() {

    return new RematchSessionId(UUID.randomUUID());
  }

  public static RematchSessionId of(final String value) {

    try {
      return new RematchSessionId(UUID.fromString(value));
    } catch (final IllegalArgumentException e) {
      throw new InvalidIdException(value);
    }
  }

}
