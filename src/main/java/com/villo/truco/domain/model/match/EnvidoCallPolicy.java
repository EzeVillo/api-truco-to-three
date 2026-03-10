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

    if (!isFirstHand) {
      throw new EnvidoNotAllowedException("El envido solo se puede cantar en la primera mano");
    }

    if (hasPlayerPlayedInCurrentHand && (status == RoundStatus.PLAYING
        || status == RoundStatus.TRUCO_IN_PROGRESS)) {
      throw new EnvidoNotAllowedException("No podes cantar envido si ya jugaste una carta");
    }

    if (isEnvidoResolved) {
      throw new EnvidoNotAllowedException("El envido ya fue resuelto en esta ronda");
    }

    if (status == RoundStatus.PLAYING && isTrucoFlowActive) {
      throw new EnvidoNotAllowedException("No podes cantar envido despues de aceptar el truco");
    }

    if (status != RoundStatus.ENVIDO_IN_PROGRESS && status != RoundStatus.PLAYING
        && status != RoundStatus.TRUCO_IN_PROGRESS) {
      throw new EnvidoNotAllowedException("No se puede cantar envido en este momento");
    }

    if (status == RoundStatus.TRUCO_IN_PROGRESS && currentTrucoCall != TrucoCall.TRUCO) {
      throw new EnvidoNotAllowedException("Solo podes cantar envido cuando el truco es TRUCO");
    }
  }

}
