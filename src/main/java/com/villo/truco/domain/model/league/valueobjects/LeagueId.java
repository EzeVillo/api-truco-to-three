package com.villo.truco.domain.model.league.valueobjects;

import com.villo.truco.domain.shared.exceptions.InvalidIdException;
import java.util.UUID;

public record LeagueId(UUID value) {

  public static LeagueId of(final String value) {

    try {
      return new LeagueId(UUID.fromString(value));
    } catch (final IllegalArgumentException e) {
      throw new InvalidIdException(value);
    }
  }

  public static LeagueId generate() {

    return new LeagueId(UUID.randomUUID());
  }

}
