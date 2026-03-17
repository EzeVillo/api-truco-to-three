package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.valueobjects.RoundStatus;
import com.villo.truco.domain.model.match.valueobjects.TrucoCall;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

final class TurnRestorationPolicy {

  private TurnRestorationPolicy() {

  }

  static PlayerId checkpointBeforeTrucoCall(final RoundStatus status, final PlayerId currentTurn,
      final PlayerId turnBeforeTrucoCall) {

    if (status != RoundStatus.TRUCO_IN_PROGRESS) {
      return currentTurn;
    }

    return turnBeforeTrucoCall;
  }

  static PlayerId checkpointBeforeEnvidoCall(final RoundStatus status, final PlayerId currentTurn,
      final PlayerId turnBeforeTrucoCall, final PlayerId turnBeforeEnvidoCall) {

    if (status == RoundStatus.ENVIDO_IN_PROGRESS) {
      return turnBeforeEnvidoCall;
    }

    if (status == RoundStatus.TRUCO_IN_PROGRESS && turnBeforeTrucoCall != null) {
      return turnBeforeTrucoCall;
    }

    return currentTurn;
  }

  static PlayerId turnAfterTrucoAccepted(final PlayerId turnBeforeTrucoCall) {

    return turnBeforeTrucoCall;
  }

  static PlayerId turnAfterEnvidoResolved(final PlayerId turnBeforeEnvidoCall) {

    return turnBeforeEnvidoCall;
  }

  static boolean shouldCancelTrucoOnEnvido(final RoundStatus previousStatus,
      final TrucoCall currentTrucoCall) {

    return previousStatus == RoundStatus.TRUCO_IN_PROGRESS && currentTrucoCall == TrucoCall.TRUCO;
  }

}
