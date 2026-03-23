package com.villo.truco.infrastructure.persistence.entities;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class LeagueParticipantId implements Serializable {

  private UUID league;
  private UUID playerId;

  public LeagueParticipantId() {

  }

  public LeagueParticipantId(UUID league, UUID playerId) {

    this.league = league;
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
    return Objects.equals(league, that.league) && Objects.equals(playerId, that.playerId);
  }

  @Override
  public int hashCode() {

    return Objects.hash(league, playerId);
  }

}
