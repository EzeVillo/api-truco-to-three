package com.villo.truco.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "cup_participants")
@IdClass(CupParticipantId.class)
public class CupParticipantJpaEntity {

  @Id
  @Column(name = "cup_id")
  private UUID cupId;

  @Id
  @Column(name = "player_id")
  private UUID playerId;

  @Column(nullable = false)
  private int ordinal;

  protected CupParticipantJpaEntity() {

  }

  public CupParticipantJpaEntity(UUID cupId, UUID playerId, int ordinal) {

    this.cupId = cupId;
    this.playerId = playerId;
    this.ordinal = ordinal;
  }

  public UUID getCupId() {

    return cupId;
  }

  public void setCupId(UUID cupId) {

    this.cupId = cupId;
  }

  public UUID getPlayerId() {

    return playerId;
  }

  public void setPlayerId(UUID playerId) {

    this.playerId = playerId;
  }

  public int getOrdinal() {

    return ordinal;
  }

  public void setOrdinal(int ordinal) {

    this.ordinal = ordinal;
  }

}
