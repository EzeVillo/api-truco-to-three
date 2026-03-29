package com.villo.truco.domain.model.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.model.match.exceptions.DeckEmptyException;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class DeckTest {

  @Test
  void shouldDeal40UniqueCards() {

    final var deck = Deck.create();
    final var dealtCards = new HashSet<Card>();

    for (int i = 0; i < 40; i++) {
      dealtCards.add(deck.dealOne());
    }

    assertThat(dealtCards).hasSize(40);
  }

  @Test
  void shouldThrowWhenDeckIsExhausted() {

    final var deck = Deck.create();

    for (int i = 0; i < 40; i++) {
      deck.dealOne();
    }

    assertThatThrownBy(deck::dealOne).isInstanceOf(DeckEmptyException.class);
  }

}