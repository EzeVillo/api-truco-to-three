package com.villo.truco.infrastructure.persistence.entities;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class TournamentWinId implements Serializable {

  private UUID tournamentId;
  private UUID playerId;

  public TournamentWinId() {

  }

  public TournamentWinId(UUID tournamentId, UUID playerId) {

    this.tournamentId = tournamentId;
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
    TournamentWinId that = (TournamentWinId) o;
    return Objects.equals(tournamentId, that.tournamentId) && Objects.equals(playerId,
        that.playerId);
  }

  @Override
  public int hashCode() {

    return Objects.hash(tournamentId, playerId);
  }

}
