package com.villo.truco.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "tournament_wins")
@IdClass(TournamentWinId.class)
public class TournamentWinJpaEntity {

  @Id
  @Column(name = "tournament_id")
  private UUID tournamentId;

  @Id
  @Column(name = "player_id")
  private UUID playerId;

  @Column(nullable = false)
  private int wins;

  protected TournamentWinJpaEntity() {

  }

  public TournamentWinJpaEntity(UUID tournamentId, UUID playerId, int wins) {

    this.tournamentId = tournamentId;
    this.playerId = playerId;
    this.wins = wins;
  }

  public UUID getTournamentId() {

    return tournamentId;
  }

  public void setTournamentId(UUID tournamentId) {

    this.tournamentId = tournamentId;
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
