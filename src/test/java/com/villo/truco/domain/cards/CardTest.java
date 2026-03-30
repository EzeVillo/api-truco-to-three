package com.villo.truco.domain.cards;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.cards.valueobjects.Suit;
import com.villo.truco.domain.shared.cards.valueobjects.exceptions.InvalidCardException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CardTest {

  @Test
  void shouldCreateCardWithValidNumberAndSuit() {

    final var card = Card.of(Suit.ESPADA, 1);

    assertThat(card.number()).isEqualTo(1);
    assertThat(card.suit()).isEqualTo(Suit.ESPADA);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 10, 11, 12})
  void shouldCreateCardForAllValidNumbers(final int number) {

    assertThatNoException().isThrownBy(() -> Card.of(Suit.ORO, number));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 8, 9, 13, -1})
  void shouldRejectInvalidNumbers(final int number) {

    assertThatThrownBy(() -> Card.of(Suit.COPA, number)).isInstanceOf(InvalidCardException.class);
  }

  @Test
  void shouldRejectNullSuit() {

    assertThatThrownBy(() -> Card.of(null, 1)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldBeEqualWhenSameSuitAndNumber() {

    final var card1 = Card.of(Suit.ESPADA, 1);
    final var card2 = Card.of(Suit.ESPADA, 1);

    assertThat(card1).isEqualTo(card2);
    assertThat(card1.hashCode()).hasSameHashCodeAs(card2.hashCode());
  }

  @Test
  void shouldNotBeEqualWhenDifferentSuit() {

    final var card1 = Card.of(Suit.ESPADA, 1);
    final var card2 = Card.of(Suit.BASTO, 1);

    assertThat(card1).isNotEqualTo(card2);
  }

  @Test
  void shouldNotBeEqualWhenDifferentNumber() {

    final var card1 = Card.of(Suit.ESPADA, 1);
    final var card2 = Card.of(Suit.ESPADA, 2);

    assertThat(card1).isNotEqualTo(card2);
  }

}