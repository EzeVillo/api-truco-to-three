package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.valueobjects.MatchStatus;

final class MatchActionSpecification {

  private MatchActionSpecification() {

  }

  static boolean canExecuteInRound(final MatchStatus status) {

    return status == MatchStatus.IN_PROGRESS;
  }

}