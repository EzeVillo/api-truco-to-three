package com.villo.truco.domain.model.bot;

import com.villo.truco.domain.model.bot.valueobjects.BotTrucoCall;

final class TrucoScoreStrategy {

  private TrucoScoreStrategy() {

  }

  static boolean shouldQYMVAM(final int rivalScore, final BotTrucoCall pendingOffer,
      final int pointsToWin) {

    return rivalExceedsIfAccepted(rivalScore, pendingOffer, pointsToWin);
  }

  static boolean noQuieroKillsRival(final int rivalScore, final BotTrucoCall pendingOffer,
      final int pointsToWin) {

    return rivalScore + pendingOffer.stakeIfRejected() > pointsToWin;
  }

  static boolean rivalWinsIfRejected(final int rivalScore, final BotTrucoCall pendingOffer,
      final int pointsToWin) {

    return rivalScore + pendingOffer.stakeIfRejected() == pointsToWin;
  }

  static boolean botWinsIfRejected(final int myScore, final BotTrucoCall pendingOffer,
      final int pointsToWin) {

    return myScore + pendingOffer.stakeIfRejected() == pointsToWin;
  }

  static boolean rivalExceedsIfAccepted(final int rivalScore, final BotTrucoCall pendingOffer,
      final int pointsToWin) {

    return rivalScore + pendingOffer.stakeIfAccepted() > pointsToWin;
  }

  static boolean botExceedsIfAccepted(final int myScore, final BotTrucoCall pendingOffer,
      final int pointsToWin) {

    return myScore + pendingOffer.stakeIfAccepted() > pointsToWin;
  }

}
