package com.villo.truco.domain.model.match;

import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.model.match.events.HandChangedEvent;
import com.villo.truco.domain.model.match.exceptions.CardNotInHandException;
import com.villo.truco.domain.model.match.valueobjects.HandId;
import com.villo.truco.domain.shared.EntityBase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class Hand extends EntityBase<HandId> {

  private final List<Card> cards;

  private Hand(final HandId id, final List<Card> cards) {

    super(id);
    this.cards = new ArrayList<>(cards);
  }

  static Hand of(final Card cardOne, final Card cardTwo, final Card cardThree) {

    return new Hand(HandId.generate(), List.of(cardOne, cardTwo, cardThree));
  }

  static Hand reconstruct(final HandId id, final List<Card> cards) {

    return new Hand(id, cards);
  }

  Card play(final Card card) {

    if (!cards.contains(card)) {
      throw new CardNotInHandException(card);
    }

    cards.remove(card);
    this.addDomainEvent(new HandChangedEvent(this.cards));
    return card;
  }

  List<Card> getCards() {

    return Collections.unmodifiableList(cards);
  }

}

