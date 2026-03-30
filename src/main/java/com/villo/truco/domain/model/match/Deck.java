package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.exceptions.DeckEmptyException;
import com.villo.truco.domain.model.match.valueobjects.DeckId;
import com.villo.truco.domain.shared.EntityBase;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.cards.valueobjects.Suit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class Deck extends EntityBase<DeckId> {

  private static final List<Integer> VALID_NUMBERS = List.of(1, 2, 3, 4, 5, 6, 7, 10, 11, 12);

  private final List<Card> cards;

  private Deck(final DeckId id, final List<Card> cards) {

    super(id);
    this.cards = cards;
  }

  static Deck create() {

    final var cards = new ArrayList<Card>();

    for (final var suit : Suit.values()) {
      for (final var number : VALID_NUMBERS) {
        cards.add(Card.of(suit, number));
      }
    }

    Collections.shuffle(cards);

    return new Deck(DeckId.generate(), cards);
  }

  Card dealOne() {

    if (cards.isEmpty()) {
      throw new DeckEmptyException();
    }

    return cards.removeLast();
  }

}