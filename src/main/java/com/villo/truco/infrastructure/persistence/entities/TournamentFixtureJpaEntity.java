package com.villo.truco.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "tournament_fixtures")
public class TournamentFixtureJpaEntity {

  @Id
  private UUID id;

  @Column(name = "tournament_id", nullable = false)
  private UUID tournamentId;

  @Column(name = "matchday_number", nullable = false)
  private int matchdayNumber;

  @Column(name = "player_one")
  private UUID playerOne;

  @Column(name = "player_two")
  private UUID playerTwo;

  @Column(name = "match_id")
  private UUID matchId;

  @Column
  private UUID winner;

  @Column(nullable = false)
  private String status;

  public TournamentFixtureJpaEntity() {

  }

  public UUID getId() {

    return id;
  }

  public void setId(UUID id) {

    this.id = id;
  }

  public UUID getTournamentId() {

    return tournamentId;
  }

  public void setTournamentId(UUID tournamentId) {

    this.tournamentId = tournamentId;
  }

  public int getMatchdayNumber() {

    return matchdayNumber;
  }

  public void setMatchdayNumber(int matchdayNumber) {

    this.matchdayNumber = matchdayNumber;
  }

  public UUID getPlayerOne() {

    return playerOne;
  }

  public void setPlayerOne(UUID playerOne) {

    this.playerOne = playerOne;
  }

  public UUID getPlayerTwo() {

    return playerTwo;
  }

  public void setPlayerTwo(UUID playerTwo) {

    this.playerTwo = playerTwo;
  }

  public UUID getMatchId() {

    return matchId;
  }

  public void setMatchId(UUID matchId) {

    this.matchId = matchId;
  }

  public UUID getWinner() {

    return winner;
  }

  public void setWinner(UUID winner) {

    this.winner = winner;
  }

  public String getStatus() {

    return status;
  }

  public void setStatus(String status) {

    this.status = status;
  }

}
