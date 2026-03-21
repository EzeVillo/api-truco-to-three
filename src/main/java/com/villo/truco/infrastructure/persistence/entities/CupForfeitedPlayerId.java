package com.villo.truco.infrastructure.persistence.entities;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class CupForfeitedPlayerId implements Serializable {

  private UUID cupId;
  private UUID playerId;

  public CupForfeitedPlayerId() {

  }

  public CupForfeitedPlayerId(UUID cupId, UUID playerId) {

    this.cupId = cupId;
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
    CupForfeitedPlayerId that = (CupForfeitedPlayerId) o;
    return Objects.equals(cupId, that.cupId) && Objects.equals(playerId, that.playerId);
  }

  @Override
  public int hashCode() {

    return Objects.hash(cupId, playerId);
  }

}
