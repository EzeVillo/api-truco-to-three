package com.villo.truco.domain.model.rematch.events;

import com.villo.truco.domain.model.rematch.valueobjects.RematchSessionId;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class RematchSessionConfirmedEvent extends RematchSessionDomainEvent {

  private final MatchId newMatchId;
  private final PlayerId newPlayerOneId;
  private final PlayerId newPlayerTwoId;
  private final int gamesToWin;

  public RematchSessionConfirmedEvent(final RematchSessionId rematchSessionId,
      final MatchId originMatchId, final MatchId newMatchId, final PlayerId newPlayerOneId,
      final PlayerId newPlayerTwoId, final int gamesToWin) {

    super("REMATCH_CONFIRMED", rematchSessionId, originMatchId);
    this.newMatchId = Objects.requireNonNull(newMatchId);
    this.newPlayerOneId = Objects.requireNonNull(newPlayerOneId);
    this.newPlayerTwoId = Objects.requireNonNull(newPlayerTwoId);
    this.gamesToWin = gamesToWin;
  }

  public MatchId getNewMatchId() {

    return newMatchId;
  }

  public PlayerId getNewPlayerOneId() {

    return newPlayerOneId;
  }

  public PlayerId getNewPlayerTwoId() {

    return newPlayerTwoId;
  }

  public int getGamesToWin() {

    return gamesToWin;
  }

}
