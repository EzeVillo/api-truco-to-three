package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.shared.DomainEventBase;

public final class GameStartedEvent extends DomainEventBase {

  private final int gameNumber;

  public GameStartedEvent(final int gameNumber) {

    super("GAME_STARTED");
    this.gameNumber = gameNumber;
  }

  public int getGameNumber() {

    return this.gameNumber;
  }

}
