package com.villo.truco.domain.model.match.valueobjects;

import com.villo.truco.domain.model.match.exceptions.InvalidMatchRulesException;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;

public record MatchRules(int gamesToWin, boolean forfeitsOnInactivity) {

  public MatchRules {

    if (gamesToWin <= 0) {
      throw new InvalidMatchRulesException("gamesToWin must be greater than zero");
    }

  }

  public static MatchRules fromGamesToPlay(final GamesToPlay gamesToPlay,
      final boolean forfeitsOnInactivity) {

    final var gamesToWin = gamesToPlay.value() / 2 + 1;
    return new MatchRules(gamesToWin, forfeitsOnInactivity);
  }

}
