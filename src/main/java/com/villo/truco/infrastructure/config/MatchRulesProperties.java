package com.villo.truco.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "truco.match")
public class MatchRulesProperties {

  private int gamesToWin = 3;
  private int pointsToWinGame = 3;

  public int getGamesToWin() {

    return gamesToWin;
  }

  public void setGamesToWin(final int gamesToWin) {

    this.gamesToWin = gamesToWin;
  }

  public int getPointsToWinGame() {

    return pointsToWinGame;
  }

  public void setPointsToWinGame(final int pointsToWinGame) {

    this.pointsToWinGame = pointsToWinGame;
  }

}
