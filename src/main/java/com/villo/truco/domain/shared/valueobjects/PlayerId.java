package com.villo.truco.domain.shared.valueobjects;

import com.villo.truco.domain.shared.exceptions.InvalidIdException;
import java.util.UUID;

public record PlayerId(UUID value) {

  public static PlayerId generate() {

    return new PlayerId(UUID.randomUUID());
  }

  public static PlayerId of(final String value) {

    try {
      return new PlayerId(UUID.fromString(value));
    } catch (final IllegalArgumentException e) {
      throw new InvalidIdException(value);
    }
  }

}
