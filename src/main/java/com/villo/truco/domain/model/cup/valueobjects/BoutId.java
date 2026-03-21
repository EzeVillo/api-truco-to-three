package com.villo.truco.domain.model.cup.valueobjects;

import com.villo.truco.domain.shared.exceptions.InvalidIdException;
import java.util.UUID;

public record BoutId(UUID value) {

  public static BoutId of(final String value) {

    try {
      return new BoutId(UUID.fromString(value));
    } catch (final IllegalArgumentException e) {
      throw new InvalidIdException(value);
    }
  }

  public static BoutId generate() {

    return new BoutId(UUID.randomUUID());
  }

}
