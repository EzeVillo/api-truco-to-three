package com.villo.truco.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "cup_forfeited_players")
@IdClass(CupForfeitedPlayerId.class)
public class CupForfeitedPlayerJpaEntity {

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cup_id")
  private CupJpaEntity cup;

  @Id
  @Column(name = "player_id")
  private UUID playerId;

  protected CupForfeitedPlayerJpaEntity() {

  }

  public CupForfeitedPlayerJpaEntity(CupJpaEntity cup, UUID playerId) {

    this.cup = cup;
    this.playerId = playerId;
  }

  public CupJpaEntity getCup() {

    return cup;
  }

  public void setCup(CupJpaEntity cup) {

    this.cup = cup;
  }

  public UUID getCupId() {

    return cup != null ? cup.getId() : null;
  }

  public UUID getPlayerId() {

    return playerId;
  }

  public void setPlayerId(UUID playerId) {

    this.playerId = playerId;
  }

}
