package com.villo.truco.domain.model.match.valueobjects;

import com.villo.truco.domain.model.match.exceptions.InvalidMatchRulesException;

public record MatchRules(int gamesToWin) {

  public MatchRules {

    if (gamesToWin <= 0) {
      throw new InvalidMatchRulesException("gamesToWin must be greater than zero");
    }

  }

  public static MatchRules fromGamesToPlay(final int gamesToPlay) {

    if (gamesToPlay != 1 && gamesToPlay != 3 && gamesToPlay != 5) {
      throw new InvalidMatchRulesException("gamesToPlay must be one of: 1, 3, 5");
    }

    final var gamesToWin = gamesToPlay / 2 + 1;
    return new MatchRules(gamesToWin);
  }

}
