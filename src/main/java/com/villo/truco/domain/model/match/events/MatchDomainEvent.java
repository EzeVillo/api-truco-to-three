package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public abstract class MatchDomainEvent extends DomainEventBase {

  private final MatchId matchId;
  private final PlayerId playerOne;
  private final PlayerId playerTwo;
  private long stateVersion;

  protected MatchDomainEvent(final String eventType, final MatchId matchId,
      final PlayerId playerOne, final PlayerId playerTwo) {

    super(eventType);
    this.matchId = Objects.requireNonNull(matchId);
    this.playerOne = Objects.requireNonNull(playerOne);
    this.playerTwo = playerTwo;
  }

  public MatchId getMatchId() {

    return matchId;
  }

  public PlayerId getPlayerOne() {

    return playerOne;
  }

  public PlayerId getPlayerTwo() {

    return playerTwo;
  }

  public long getStateVersion() {

    return stateVersion;
  }

  public void setStateVersion(long stateVersion) {

    this.stateVersion = stateVersion;
  }

  public PlayerId resolvePlayer(final PlayerSeat seat) {

    return seat == PlayerSeat.PLAYER_ONE ? playerOne : playerTwo;
  }

}
