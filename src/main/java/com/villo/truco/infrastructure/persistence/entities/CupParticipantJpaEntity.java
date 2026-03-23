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
@Table(name = "cup_participants")
@IdClass(CupParticipantId.class)
public class CupParticipantJpaEntity {

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cup_id")
  private CupJpaEntity cup;

  @Id
  @Column(name = "player_id")
  private UUID playerId;

  @Column(nullable = false)
  private int ordinal;

  protected CupParticipantJpaEntity() {

  }

  public CupParticipantJpaEntity(CupJpaEntity cup, UUID playerId, int ordinal) {

    this.cup = cup;
    this.playerId = playerId;
    this.ordinal = ordinal;
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

  public int getOrdinal() {

    return ordinal;
  }

  public void setOrdinal(int ordinal) {

    this.ordinal = ordinal;
  }

}
