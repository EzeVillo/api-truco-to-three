package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.valueobjects.RoundStatus;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.cards.valueobjects.Suit;

final class AnchoDeEspadaImmediateClosurePolicy {

  private AnchoDeEspadaImmediateClosurePolicy() {

  }

  static boolean shouldCloseRound(final RoundStatus status, final Card playedCard,
      final int playedHandsCount, final boolean firstHandWasTie) {

    if (status != RoundStatus.PLAYING || !isAnchoDeEspada(playedCard)) {
      return false;
    }

    return playedHandsCount == 2 || (playedHandsCount == 1 && firstHandWasTie);
  }

  private static boolean isAnchoDeEspada(final Card card) {

    return card.number() == 1 && card.suit() == Suit.ESPADA;
  }

}
