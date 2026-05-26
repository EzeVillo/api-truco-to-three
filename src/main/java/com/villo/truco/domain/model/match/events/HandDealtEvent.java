package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import java.util.List;
import java.util.Map;

public final class HandDealtEvent extends DomainEventBase {

  private final Map<PlayerSeat, List<Card>> seatToCards;

  public HandDealtEvent(final Map<PlayerSeat, List<Card>> seatToCards) {

    super("HAND_DEALT");
    this.seatToCards = Map.copyOf(seatToCards);
  }

  public Map<PlayerSeat, List<Card>> getSeatToCards() {

    return this.seatToCards;
  }

  public List<Card> getCardsForSeat(final PlayerSeat seat) {

    return this.seatToCards.getOrDefault(seat, List.of());
  }

}
