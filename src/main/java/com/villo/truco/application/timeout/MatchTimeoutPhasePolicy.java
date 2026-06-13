package com.villo.truco.application.timeout;

import com.villo.truco.domain.model.match.valueobjects.MatchStatus;

public final class MatchTimeoutPhasePolicy {

  public TimeoutPhase phaseOf(final MatchStatus status) {

    return switch (status) {
      case WAITING_FOR_PLAYERS, READY -> TimeoutPhase.LOBBY;
      case IN_PROGRESS -> TimeoutPhase.PLAY;
      case FINISHED, CANCELLED -> TimeoutPhase.NONE;
    };
  }

}
