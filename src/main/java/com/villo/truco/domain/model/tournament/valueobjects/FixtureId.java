package com.villo.truco.domain.model.tournament.valueobjects;

import com.villo.truco.domain.model.match.valueobjects.exceptions.InvalidIdException;
import java.util.UUID;

public record FixtureId(UUID value) {

  public static FixtureId of(final String value) {

    try {
      return new FixtureId(UUID.fromString(value));
    } catch (final IllegalArgumentException e) {
      throw new InvalidIdException(value);
    }
  }

  public static FixtureId generate() {

    return new FixtureId(UUID.randomUUID());
  }

}
