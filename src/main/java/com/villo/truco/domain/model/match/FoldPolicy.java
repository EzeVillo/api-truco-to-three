package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.exceptions.FoldNotAllowedException;

final class FoldPolicy {

  private FoldPolicy() {

  }

  static void validateFoldAllowed(final boolean isMano, final boolean isFirstHand,
      final boolean isEnvidoResolved, final boolean isTrucoCalled) {

    if (isMano && isFirstHand && !isEnvidoResolved && !isTrucoCalled) {
      throw new FoldNotAllowedException(
          "No podes irte al mazo siendo mano en primera mano sin envido cantado. "
              + "Solo se permite si el truco fue cantado y aceptado.");
    }
  }

}
