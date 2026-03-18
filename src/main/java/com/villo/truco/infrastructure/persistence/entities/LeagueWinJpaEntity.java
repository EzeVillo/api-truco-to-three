package com.villo.truco.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "league_wins")
@IdClass(LeagueWinId.class)
public class LeagueWinJpaEntity {

  @Id
  @Column(name = "league_id")
  private UUID leagueId;

  @Id
  @Column(name = "player_id")
  private UUID playerId;

  @Column(nullable = false)
  private int wins;

  protected LeagueWinJpaEntity() {

  }

  public LeagueWinJpaEntity(UUID leagueId, UUID playerId, int wins) {

    this.leagueId = leagueId;
    this.playerId = playerId;
    this.wins = wins;
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

  public int getWins() {

    return wins;
  }

  public void setWins(int wins) {

    this.wins = wins;
  }

}
