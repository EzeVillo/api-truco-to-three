package com.villo.truco.domain.model.cup.valueobjects;

import com.villo.truco.domain.shared.exceptions.InvalidIdException;
import java.util.UUID;

public record CupId(UUID value) {

  public static CupId of(final String value) {

    try {
      return new CupId(UUID.fromString(value));
    } catch (final IllegalArgumentException e) {
      throw new InvalidIdException(value);
    }
  }

  public static CupId generate() {

    return new CupId(UUID.randomUUID());
  }

}
