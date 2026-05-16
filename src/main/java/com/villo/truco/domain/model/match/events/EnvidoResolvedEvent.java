package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.model.match.valueobjects.EnvidoResponse;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.DomainEventBase;

public final class EnvidoResolvedEvent extends DomainEventBase {

  private final EnvidoResponse response;
  private final PlayerSeat winnerSeat;
  private final Integer pointsMano;
  private final Integer pointsPie;
  private final boolean manoWon;

  public EnvidoResolvedEvent(final EnvidoResponse response, final PlayerSeat winnerSeat,
      final Integer pointsMano, final Integer pointsPie, final boolean manoWon) {

    super("ENVIDO_RESOLVED");
    this.response = response;
    this.winnerSeat = winnerSeat;
    this.pointsMano = pointsMano;
    this.pointsPie = pointsPie;
    this.manoWon = manoWon;
  }

  public EnvidoResponse getResponse() {

    return this.response;
  }

  public PlayerSeat getWinnerSeat() {

    return this.winnerSeat;
  }

  public Integer getPointsMano() {

    return this.pointsMano;
  }

  public Integer getPointsPie() {

    return this.pointsPie;
  }

  public boolean isManoWon() {

    return this.manoWon;
  }

}
