package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.DomainEventBase;

public final class HandResolvedEvent extends DomainEventBase {

  private final Card cardPlayerOne;
  private final Card cardPlayerTwo;
  private final PlayerSeat winnerSeat;

  public HandResolvedEvent(final Card cardPlayerOne, final Card cardPlayerTwo,
      final PlayerSeat winnerSeat) {

    super("HAND_RESOLVED");
    this.cardPlayerOne = cardPlayerOne;
    this.cardPlayerTwo = cardPlayerTwo;
    this.winnerSeat = winnerSeat;
  }

  public Card getCardPlayerOne() {

    return this.cardPlayerOne;
  }

  public Card getCardPlayerTwo() {

    return this.cardPlayerTwo;
  }

  public PlayerSeat getWinnerSeat() {

    return this.winnerSeat;
  }

}
