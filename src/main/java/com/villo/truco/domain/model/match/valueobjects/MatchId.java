package com.villo.truco.domain.model.match.valueobjects;

import com.villo.truco.domain.shared.exceptions.InvalidIdException;
import java.util.UUID;

public record MatchId(UUID value) {

  public static MatchId of(final String value) {

    try {
      return new MatchId(UUID.fromString(value));
    } catch (final IllegalArgumentException e) {
      throw new InvalidIdException(value);
    }
  }

  public static MatchId generate() {

    return new MatchId(UUID.randomUUID());
  }

}
