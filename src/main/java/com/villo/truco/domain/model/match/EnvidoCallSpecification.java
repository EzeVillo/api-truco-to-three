package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.valueobjects.RoundStatus;
import com.villo.truco.domain.model.match.valueobjects.TrucoCall;

final class EnvidoCallSpecification {

  private EnvidoCallSpecification() {

  }

  static Decision evaluate(final RoundStatus status, final boolean isFirstHand,
      final boolean hasPlayerPlayedInCurrentHand, final boolean isEnvidoResolved,
      final boolean isTrucoFlowActive, final TrucoCall currentTrucoCall) {

    if (!isFirstHand) {
      return Decision.reject("El envido solo se puede cantar en la primera mano");
    }

    if (hasPlayerPlayedInCurrentHand && (status == RoundStatus.PLAYING
        || status == RoundStatus.TRUCO_IN_PROGRESS)) {
      return Decision.reject("No podes cantar envido si ya jugaste una carta");
    }

    if (isEnvidoResolved) {
      return Decision.reject("El envido ya fue resuelto en esta ronda");
    }

    if (status == RoundStatus.PLAYING && isTrucoFlowActive) {
      return Decision.reject("No podes cantar envido despues de aceptar el truco");
    }

    if (status != RoundStatus.ENVIDO_IN_PROGRESS && status != RoundStatus.PLAYING
        && status != RoundStatus.TRUCO_IN_PROGRESS) {
      return Decision.reject("No se puede cantar envido en este momento");
    }

    if (status == RoundStatus.TRUCO_IN_PROGRESS && currentTrucoCall != TrucoCall.TRUCO) {
      return Decision.reject("Solo podes cantar envido cuando el truco es TRUCO");
    }

    return Decision.allow();
  }

  record Decision(boolean satisfied, String reason) {

    static Decision allow() {

      return new Decision(true, null);
    }

    static Decision reject(final String reason) {

      return new Decision(false, reason);
    }

  }

}