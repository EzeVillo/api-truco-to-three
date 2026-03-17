package com.villo.truco.domain.shared.valueobjects;

import com.villo.truco.domain.shared.DomainException;

public record GamesToPlay(int value) {

  public GamesToPlay {

    if (value != 1 && value != 3 && value != 5) {
      throw new InvalidGamesToPlayException(value);
    }
  }

  public static GamesToPlay of(final int value) {

    return new GamesToPlay(value);
  }

  private static final class InvalidGamesToPlayException extends DomainException {

    InvalidGamesToPlayException(final int value) {

      super("gamesToPlay must be one of: 1, 3, 5, but was: " + value);
    }

  }

}
