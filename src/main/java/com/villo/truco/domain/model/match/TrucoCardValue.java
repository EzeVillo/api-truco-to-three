package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.valueobjects.Card;
import com.villo.truco.domain.model.match.valueobjects.Suit;

final class TrucoCardValue {

    private TrucoCardValue() {

    }

    static int of(final Card card) {

        final var suit = card.suit();
        final var number = card.number();

        if (number == 1 && suit == Suit.ESPADA) {
            return 14;
        }
        if (number == 1 && suit == Suit.BASTO) {
            return 13;
        }
        if (number == 7 && suit == Suit.ESPADA) {
            return 12;
        }
        if (number == 7 && suit == Suit.ORO) {
            return 11;
        }
        if (number == 3) {
            return 10;
        }
        if (number == 2) {
            return 9;
        }
        if (number == 1) {
            return 8;
        }
        if (number == 12) {
            return 7;
        }
        if (number == 11) {
            return 6;
        }
        if (number == 10) {
            return 5;
        }
        if (number == 7) {
            return 4;
        }
        if (number == 6) {
            return 3;
        }
        if (number == 5) {
            return 2;
        }
        if (number == 4) {
            return 1;
        }

        throw new IllegalStateException("Unexpected card: " + card);
    }

}
