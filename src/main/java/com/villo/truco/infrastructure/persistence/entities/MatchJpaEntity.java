package com.villo.truco.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "matches")
public class MatchJpaEntity {

  @Id
  private UUID id;

  @Column(name = "player_one", nullable = false)
  private UUID playerOne;

  @Column(name = "player_two")
  private UUID playerTwo;

  @Column(name = "invite_code")
  private String inviteCode;

  @Column(nullable = false)
  private String status;

  @Column(name = "games_to_win", nullable = false)
  private int gamesToWin;

  @Column(name = "games_won_player_one", nullable = false)
  private int gamesWonPlayerOne;

  @Column(name = "games_won_player_two", nullable = false)
  private int gamesWonPlayerTwo;

  @Column(name = "game_number", nullable = false)
  private int gameNumber;

  @Column(name = "score_player_one", nullable = false)
  private int scorePlayerOne;

  @Column(name = "score_player_two", nullable = false)
  private int scorePlayerTwo;

  @Column(name = "round_number", nullable = false)
  private int roundNumber;

  @Column(name = "ready_player_one", nullable = false)
  private boolean readyPlayerOne;

  @Column(name = "ready_player_two", nullable = false)
  private boolean readyPlayerTwo;

  @Column(name = "first_mano_of_game")
  private UUID firstManoOfGame;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "current_round", columnDefinition = "jsonb")
  private RoundData currentRound;

  @Version
  private int version;

  public MatchJpaEntity() {

  }

  public UUID getId() {

    return id;
  }

  public void setId(UUID id) {

    this.id = id;
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

  public String getInviteCode() {

    return inviteCode;
  }

  public void setInviteCode(String inviteCode) {

    this.inviteCode = inviteCode;
  }

  public String getStatus() {

    return status;
  }

  public void setStatus(String status) {

    this.status = status;
  }

  public int getGamesToWin() {

    return gamesToWin;
  }

  public void setGamesToWin(int gamesToWin) {

    this.gamesToWin = gamesToWin;
  }

  public int getGamesWonPlayerOne() {

    return gamesWonPlayerOne;
  }

  public void setGamesWonPlayerOne(int gamesWonPlayerOne) {

    this.gamesWonPlayerOne = gamesWonPlayerOne;
  }

  public int getGamesWonPlayerTwo() {

    return gamesWonPlayerTwo;
  }

  public void setGamesWonPlayerTwo(int gamesWonPlayerTwo) {

    this.gamesWonPlayerTwo = gamesWonPlayerTwo;
  }

  public int getGameNumber() {

    return gameNumber;
  }

  public void setGameNumber(int gameNumber) {

    this.gameNumber = gameNumber;
  }

  public int getScorePlayerOne() {

    return scorePlayerOne;
  }

  public void setScorePlayerOne(int scorePlayerOne) {

    this.scorePlayerOne = scorePlayerOne;
  }

  public int getScorePlayerTwo() {

    return scorePlayerTwo;
  }

  public void setScorePlayerTwo(int scorePlayerTwo) {

    this.scorePlayerTwo = scorePlayerTwo;
  }

  public int getRoundNumber() {

    return roundNumber;
  }

  public void setRoundNumber(int roundNumber) {

    this.roundNumber = roundNumber;
  }

  public boolean isReadyPlayerOne() {

    return readyPlayerOne;
  }

  public void setReadyPlayerOne(boolean readyPlayerOne) {

    this.readyPlayerOne = readyPlayerOne;
  }

  public boolean isReadyPlayerTwo() {

    return readyPlayerTwo;
  }

  public void setReadyPlayerTwo(boolean readyPlayerTwo) {

    this.readyPlayerTwo = readyPlayerTwo;
  }

  public UUID getFirstManoOfGame() {

    return firstManoOfGame;
  }

  public void setFirstManoOfGame(UUID firstManoOfGame) {

    this.firstManoOfGame = firstManoOfGame;
  }

  public RoundData getCurrentRound() {

    return currentRound;
  }

  public void setCurrentRound(RoundData currentRound) {

    this.currentRound = currentRound;
  }

  public int getVersion() {

    return version;
  }

  public void setVersion(int version) {

    this.version = version;
  }

}
