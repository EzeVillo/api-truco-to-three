package com.villo.truco.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "cup_forfeited_players")
@IdClass(CupForfeitedPlayerId.class)
public class CupForfeitedPlayerJpaEntity {

  @Id
  @Column(name = "cup_id")
  private UUID cupId;

  @Id
  @Column(name = "player_id")
  private UUID playerId;

  protected CupForfeitedPlayerJpaEntity() {

  }

  public CupForfeitedPlayerJpaEntity(UUID cupId, UUID playerId) {

    this.cupId = cupId;
    this.playerId = playerId;
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

}
