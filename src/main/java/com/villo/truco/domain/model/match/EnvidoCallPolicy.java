package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.exceptions.EnvidoNotAllowedException;
import com.villo.truco.domain.model.match.valueobjects.RoundStatus;
import com.villo.truco.domain.model.match.valueobjects.TrucoCall;

final class EnvidoCallPolicy {

  private EnvidoCallPolicy() {

  }

  static void validateCanCallEnvido(final RoundStatus status, final boolean isFirstHand,
      final boolean hasPlayerPlayedInCurrentHand, final boolean isEnvidoResolved,
      final boolean isTrucoFlowActive, final TrucoCall currentTrucoCall) {

    final var decision = EnvidoCallSpecification.evaluate(status, isFirstHand,
        hasPlayerPlayedInCurrentHand, isEnvidoResolved, isTrucoFlowActive, currentTrucoCall);

    if (!decision.satisfied()) {
      throw new EnvidoNotAllowedException(decision.reason());
    }
  }

}
