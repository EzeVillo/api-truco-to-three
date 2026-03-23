package com.villo.truco.infrastructure.persistence.entities;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class CupParticipantId implements Serializable {

  private UUID cup;
  private UUID playerId;

  public CupParticipantId() {

  }

  public CupParticipantId(UUID cup, UUID playerId) {

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
    CupParticipantId that = (CupParticipantId) o;
    return Objects.equals(cup, that.cup) && Objects.equals(playerId, that.playerId);
  }

  @Override
  public int hashCode() {

    return Objects.hash(cup, playerId);
  }

}
