package com.villo.truco.infrastructure.persistence.entities;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class LeagueParticipantId implements Serializable {

  private UUID leagueId;
  private UUID playerId;

  public LeagueParticipantId() {

  }

  public LeagueParticipantId(UUID leagueId, UUID playerId) {

    this.leagueId = leagueId;
    this.playerId = playerId;
  }

  @Override
  public boolean equals(Object o) {

    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LeagueParticipantId that = (LeagueParticipantId) o;
    return Objects.equals(leagueId, that.leagueId) && Objects.equals(playerId, that.playerId);
  }

  @Override
  public int hashCode() {

    return Objects.hash(leagueId, playerId);
  }

}
