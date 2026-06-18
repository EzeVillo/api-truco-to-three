package com.villo.truco.domain.model.bot;

import com.villo.truco.domain.model.bot.valueobjects.BotTrucoCall;

public final class TrucoScoreStrategy {

  private TrucoScoreStrategy() {

  }

  public static boolean shouldQYMVAM(final int rivalScore, final BotTrucoCall pendingOffer,
      final int pointsToWin) {

    return rivalExceedsIfAccepted(rivalScore, pendingOffer, pointsToWin);
  }

  public static boolean noQuieroKillsRival(final int rivalScore, final BotTrucoCall pendingOffer,
      final int pointsToWin) {

    return rivalScore + pendingOffer.stakeIfRejected() > pointsToWin;
  }

  public static boolean rivalWinsIfRejected(final int rivalScore, final BotTrucoCall pendingOffer,
      final int pointsToWin) {

    return rivalScore + pendingOffer.stakeIfRejected() == pointsToWin;
  }

  public static boolean botWinsIfRejected(final int myScore, final BotTrucoCall pendingOffer,
      final int pointsToWin) {

    return myScore + pendingOffer.stakeIfRejected() == pointsToWin;
  }

  public static boolean rivalExceedsIfAccepted(final int rivalScore, final BotTrucoCall pendingOffer,
      final int pointsToWin) {

    return rivalScore + pendingOffer.stakeIfAccepted() > pointsToWin;
  }

  public static boolean botExceedsIfAccepted(final int myScore, final BotTrucoCall pendingOffer,
      final int pointsToWin) {

    return myScore + pendingOffer.stakeIfAccepted() > pointsToWin;
  }

}
