package com.villo.truco.domain.model.tournament.valueobjects;

import com.villo.truco.domain.shared.exceptions.InvalidIdException;
import java.util.UUID;

public record TournamentId(UUID value) {

  public static TournamentId of(final String value) {

    try {
      return new TournamentId(UUID.fromString(value));
    } catch (final IllegalArgumentException e) {
      throw new InvalidIdException(value);
    }
  }

  public static TournamentId generate() {

    return new TournamentId(UUID.randomUUID());
  }

}
