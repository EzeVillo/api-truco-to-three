package com.villo.truco.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "league_participants")
@IdClass(LeagueParticipantId.class)
public class LeagueParticipantJpaEntity {

  @Id
  @Column(name = "league_id")
  private UUID leagueId;

  @Id
  @Column(name = "player_id")
  private UUID playerId;

  @Column(nullable = false)
  private int ordinal;

  protected LeagueParticipantJpaEntity() {

  }

  public LeagueParticipantJpaEntity(UUID leagueId, UUID playerId, int ordinal) {

    this.leagueId = leagueId;
    this.playerId = playerId;
    this.ordinal = ordinal;
  }

  public UUID getLeagueId() {

    return leagueId;
  }

  public void setLeagueId(UUID leagueId) {

    this.leagueId = leagueId;
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
