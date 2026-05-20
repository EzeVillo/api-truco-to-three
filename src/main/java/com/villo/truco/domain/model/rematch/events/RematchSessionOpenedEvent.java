package com.villo.truco.domain.model.rematch.events;

import com.villo.truco.domain.model.rematch.valueobjects.RematchSessionId;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.Objects;

public final class RematchSessionOpenedEvent extends RematchSessionDomainEvent {

  private final PlayerId playerOneId;
  private final PlayerId playerTwoId;
  private final Instant expiresAt;
  private final boolean playerOneIsBot;
  private final boolean playerTwoIsBot;

  public RematchSessionOpenedEvent(final RematchSessionId rematchSessionId,
      final MatchId originMatchId, final PlayerId playerOneId, final PlayerId playerTwoId,
      final Instant expiresAt, final boolean playerOneIsBot, final boolean playerTwoIsBot) {

    super("REMATCH_AVAILABLE", rematchSessionId, originMatchId);
    this.playerOneId = Objects.requireNonNull(playerOneId);
    this.playerTwoId = Objects.requireNonNull(playerTwoId);
    this.expiresAt = Objects.requireNonNull(expiresAt);
    this.playerOneIsBot = playerOneIsBot;
    this.playerTwoIsBot = playerTwoIsBot;
  }

  public PlayerId getPlayerOneId() {

    return playerOneId;
  }

  public PlayerId getPlayerTwoId() {

    return playerTwoId;
  }

  public Instant getExpiresAt() {

    return expiresAt;
  }

  public boolean isPlayerOneIsBot() {

    return playerOneIsBot;
  }

  public boolean isPlayerTwoIsBot() {

    return playerTwoIsBot;
  }

}
