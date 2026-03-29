package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.DomainEventBase;
import java.util.List;

public final class HandChangedEvent extends DomainEventBase {

  private final List<Card> remainingCards;

  public HandChangedEvent(final List<Card> remainingCards) {

    super("HAND_CHANGED");
    this.remainingCards = List.copyOf(remainingCards);
  }

  public List<Card> getRemainingCards() {

    return this.remainingCards;
  }

}
