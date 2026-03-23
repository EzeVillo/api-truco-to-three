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
@Table(name = "league_participants")
@IdClass(LeagueParticipantId.class)
public class LeagueParticipantJpaEntity {

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "league_id")
  private LeagueJpaEntity league;

  @Id
  @Column(name = "player_id")
  private UUID playerId;

  @Column(nullable = false)
  private int ordinal;

  protected LeagueParticipantJpaEntity() {

  }

  public LeagueParticipantJpaEntity(LeagueJpaEntity league, UUID playerId, int ordinal) {

    this.league = league;
    this.playerId = playerId;
    this.ordinal = ordinal;
  }

  public LeagueJpaEntity getLeague() {

    return league;
  }

  public void setLeague(LeagueJpaEntity league) {

    this.league = league;
  }

  public UUID getLeagueId() {

    return league != null ? league.getId() : null;
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
