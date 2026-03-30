package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.cards.valueobjects.Card;

public final class CardPlayedEvent extends DomainEventBase {

  private final PlayerSeat seat;
  private final Card card;

  public CardPlayedEvent(final PlayerSeat seat, final Card card) {

    super("CARD_PLAYED");
    this.seat = seat;
    this.card = card;
  }

  public PlayerSeat getSeat() {

    return this.seat;
  }

  public Card getCard() {

    return this.card;
  }

}
