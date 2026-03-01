package com.villo.truco.domain.model.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.domain.model.match.exceptions.CardNotInHandException;
import com.villo.truco.domain.model.match.valueobjects.Card;
import com.villo.truco.domain.model.match.valueobjects.Suit;
import org.junit.jupiter.api.Test;

class HandTest {

    private static final Card CARD_1 = Card.of(Suit.ESPADA, 1);
    private static final Card CARD_2 = Card.of(Suit.BASTO, 2);
    private static final Card CARD_3 = Card.of(Suit.ORO, 3);

    @Test
    void shouldPlayCardThatIsInHand() {

        final var hand = Hand.of(CARD_1, CARD_2, CARD_3);

        final var played = hand.play(CARD_1);

        assertThat(played).isEqualTo(CARD_1);
        assertThat(hand.getCards()).doesNotContain(CARD_1);
    }

    @Test
    void shouldThrowWhenPlayingCardNotInHand() {

        final var hand = Hand.of(CARD_1, CARD_2, CARD_3);
        final var foreignCard = Card.of(Suit.COPA, 7);

        assertThatThrownBy(() -> hand.play(foreignCard)).isInstanceOf(CardNotInHandException.class);
    }

    @Test
    void shouldNotBeAbleToPlaySameCardTwice() {

        final var hand = Hand.of(CARD_1, CARD_2, CARD_3);

        hand.play(CARD_1);

        assertThatThrownBy(() -> hand.play(CARD_1)).isInstanceOf(CardNotInHandException.class);
    }

    @Test
    void shouldReturnUnmodifiableCards() {

        final var hand = Hand.of(CARD_1, CARD_2, CARD_3);

        assertThatThrownBy(() -> hand.getCards().removeFirst()).isInstanceOf(
            UnsupportedOperationException.class);
    }

}