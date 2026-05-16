package com.villo.truco.profile.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

@Entity
@Table(name = "player_stats")
public class PlayerStatsJpaEntity {

  @Id
  @Column(name = "player_id", nullable = false)
  private UUID playerId;

  @Column(name = "matches_played", nullable = false)
  private int matchesPlayed;

  @Column(name = "matches_won", nullable = false)
  private int matchesWon;

  @Column(name = "matches_lost", nullable = false)
  private int matchesLost;

  @Version
  private int version;

  public UUID getPlayerId() {

    return this.playerId;
  }

  public void setPlayerId(final UUID playerId) {

    this.playerId = playerId;
  }

  public int getMatchesPlayed() {

    return this.matchesPlayed;
  }

  public void setMatchesPlayed(final int matchesPlayed) {

    this.matchesPlayed = matchesPlayed;
  }

  public int getMatchesWon() {

    return this.matchesWon;
  }

  public void setMatchesWon(final int matchesWon) {

    this.matchesWon = matchesWon;
  }

  public int getMatchesLost() {

    return this.matchesLost;
  }

  public void setMatchesLost(final int matchesLost) {

    this.matchesLost = matchesLost;
  }

  public int getVersion() {

    return this.version;
  }

  public void setVersion(final int version) {

    this.version = version;
  }

}
