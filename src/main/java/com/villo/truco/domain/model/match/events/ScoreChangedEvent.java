package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public final class ScoreChangedEvent extends MatchDomainEvent {

  private final int scorePlayerOne;
  private final int scorePlayerTwo;

  public ScoreChangedEvent(final MatchId matchId, final PlayerId playerOne,
      final PlayerId playerTwo, final int scorePlayerOne, final int scorePlayerTwo) {

    super("SCORE_CHANGED", matchId, playerOne, playerTwo);
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
