package com.villo.truco.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "cup_bouts")
public class CupBoutJpaEntity {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cup_id", nullable = false)
  private CupJpaEntity cup;

  @Column(name = "round_number", nullable = false)
  private int roundNumber;

  @Column(name = "bracket_position", nullable = false)
  private int bracketPosition;

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

  public CupBoutJpaEntity() {

  }

  public UUID getId() {

    return id;
  }

  public void setId(UUID id) {

    this.id = id;
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

  public int getRoundNumber() {

    return roundNumber;
  }

  public void setRoundNumber(int roundNumber) {

    this.roundNumber = roundNumber;
  }

  public int getBracketPosition() {

    return bracketPosition;
  }

  public void setBracketPosition(int bracketPosition) {

    this.bracketPosition = bracketPosition;
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
