package com.villo.truco.domain.model.match;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.villo.truco.domain.model.match.valueobjects.Card;
import com.villo.truco.domain.model.match.valueobjects.Suit;
import java.util.List;
import org.junit.jupiter.api.Test;

class EnvidoCalculatorTest {

    @Test
    void shouldReturnHighestCardValueWhenNoMatchingSuit() {

        final var cards = List.of(Card.of(Suit.ESPADA, 5), Card.of(Suit.BASTO, 3),
            Card.of(Suit.ORO, 2));

        assertThat(EnvidoCalculator.calculate(cards)).isEqualTo(5);
    }

    @Test
    void shouldReturnZeroWhenAllCardsAreFiguresAndNoMatchingSuit() {

        final var cards = List.of(Card.of(Suit.ESPADA, 10), Card.of(Suit.BASTO, 11),
            Card.of(Suit.ORO, 12));

        assertThat(EnvidoCalculator.calculate(cards)).isZero();
    }

    @Test
    void shouldSumTwoCardsOfSameSuitPlusTwenty() {

        final var cards = List.of(Card.of(Suit.ESPADA, 5), Card.of(Suit.ESPADA, 3),
            Card.of(Suit.ORO, 7));

        assertThat(EnvidoCalculator.calculate(cards)).isEqualTo(28);
    }

    @Test
    void shouldUseBestTwoCardsWhenThreeOfSameSuit() {

        final var cards = List.of(Card.of(Suit.ESPADA, 5), Card.of(Suit.ESPADA, 3),
            Card.of(Suit.ESPADA, 7));

        assertThat(EnvidoCalculator.calculate(cards)).isEqualTo(32);
    }

    @Test
    void figuresShouldCountAsZeroInEnvido() {

        final var cards = List.of(Card.of(Suit.ESPADA, 12), Card.of(Suit.ESPADA, 7),
            Card.of(Suit.ORO, 4));

        assertThat(EnvidoCalculator.calculate(cards)).isEqualTo(27);
    }

    @Test
    void twoFiguresOfSameSuitShouldGiveTwenty() {

        final var cards = List.of(Card.of(Suit.BASTO, 10), Card.of(Suit.BASTO, 11),
            Card.of(Suit.ORO, 3));

        assertThat(EnvidoCalculator.calculate(cards)).isEqualTo(20);
    }

    @Test
    void shouldReturnMaxEnvidoScore() {

        final var cards = List.of(Card.of(Suit.ESPADA, 7), Card.of(Suit.ESPADA, 6),
            Card.of(Suit.ORO, 1));

        assertThat(EnvidoCalculator.calculate(cards)).isEqualTo(33);
    }

}