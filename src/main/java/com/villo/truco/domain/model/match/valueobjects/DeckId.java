package com.villo.truco.domain.model.match.valueobjects;

import java.util.UUID;

public record DeckId(UUID value) {

  public static DeckId generate() {

    return new DeckId(UUID.randomUUID());
  }

}
