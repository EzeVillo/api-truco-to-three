package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.model.match.valueobjects.Card;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.DomainEventBase;
import java.util.List;

public final class PlayerHandUpdatedEvent extends DomainEventBase implements SeatTargetedEvent {

  private final PlayerSeat seat;
  private final List<Card> cards;

  public PlayerHandUpdatedEvent(final PlayerSeat seat, final List<Card> cards) {

    super("PLAYER_HAND_UPDATED");
    this.seat = seat;
    this.cards = List.copyOf(cards);
  }

  @Override
  public PlayerSeat getTargetSeat() {

    return this.seat;
  }

  public PlayerSeat getSeat() {

    return this.seat;
  }

  public List<Card> getCards() {

    return this.cards;
  }

}
