package com.villo.truco.domain.model.bot.decision;

public final class MatchArithmetic {

  private final int myScore;
  private final int rivalScore;
  private final int pointsToWin;

  public MatchArithmetic(final int myScore, final int rivalScore, final int pointsToWin) {

    this.myScore = myScore;
    this.rivalScore = rivalScore;
    this.pointsToWin = pointsToWin;
  }

  public boolean rivalBustsIfAccepts(final int stakeAccepted) {

    return rivalScore + stakeAccepted > pointsToWin;
  }

  public boolean rivalBustsIfWins(final int stakeAccepted) {

    return rivalBustsIfAccepts(stakeAccepted);
  }

  public boolean rivalBustsIfRejects(final int stakeRejected) {

    return rivalScore + stakeRejected > pointsToWin;
  }

  public boolean botBustsIfAccepts(final int stakeAccepted) {

    return myScore + stakeAccepted > pointsToWin;
  }

  public boolean botBustsIfWins(final int stakeAccepted) {

    return botBustsIfAccepts(stakeAccepted);
  }

  public boolean botReachesExact(final int points) {

    return myScore + points == pointsToWin;
  }

  public boolean rivalReachesExact(final int points) {

    return rivalScore + points == pointsToWin;
  }

  /** Rival gana o se pasa si bot pierde la apuesta aceptada. */
  public boolean rivalWinsMatchIfBotLoses(final int stakeAccepted) {

    return rivalScore + stakeAccepted >= pointsToWin;
  }

}
