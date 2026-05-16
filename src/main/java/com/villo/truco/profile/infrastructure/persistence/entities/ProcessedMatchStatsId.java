package com.villo.truco.profile.infrastructure.persistence.entities;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class ProcessedMatchStatsId implements Serializable {

  private UUID playerId;
  private UUID matchId;

  public ProcessedMatchStatsId() {

  }

  public ProcessedMatchStatsId(final UUID playerId, final UUID matchId) {

    this.playerId = playerId;
    this.matchId = matchId;
  }

  public UUID getPlayerId() {

    return this.playerId;
  }

  public UUID getMatchId() {

    return this.matchId;
  }

  @Override
  public boolean equals(final Object o) {

    if (this == o) {
      return true;
    }
    if (!(o instanceof ProcessedMatchStatsId other)) {
      return false;
    }
    return Objects.equals(this.playerId, other.playerId) && Objects.equals(this.matchId,
        other.matchId);
  }

  @Override
  public int hashCode() {

    return Objects.hash(this.playerId, this.matchId);
  }

}
