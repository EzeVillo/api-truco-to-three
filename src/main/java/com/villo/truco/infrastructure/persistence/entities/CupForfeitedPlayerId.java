package com.villo.truco.infrastructure.persistence.entities;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class CupForfeitedPlayerId implements Serializable {

  private UUID cup;
  private UUID playerId;

  public CupForfeitedPlayerId() {

  }

  public CupForfeitedPlayerId(UUID cup, UUID playerId) {

    this.cup = cup;
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
    return Objects.equals(cup, that.cup) && Objects.equals(playerId, that.playerId);
  }

  @Override
  public int hashCode() {

    return Objects.hash(cup, playerId);
  }

}
