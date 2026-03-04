package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.shared.DomainEventBase;

public final class ScoreChangedEvent extends DomainEventBase {

  private final int scorePlayerOne;
  private final int scorePlayerTwo;

  public ScoreChangedEvent(final int scorePlayerOne, final int scorePlayerTwo) {

    super("SCORE_CHANGED");
    this.scorePlayerOne = scorePlayerOne;
    this.scorePlayerTwo = scorePlayerTwo;
  }

  public int getScorePlayerOne() {

    return this.scorePlayerOne;
  }

  public int getScorePlayerTwo() {

    return this.scorePlayerTwo;
  }

}
