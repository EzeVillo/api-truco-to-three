package com.villo.truco.domain.model.bot.valueobjects;

import java.util.List;

public record BotMatchView(GameContext game, TrucoContext truco, EnvidoContext envido) {

  public record GameContext(List<BotCard> myCards, int myScore, int rivalScore,
                            BotCard rivalCardPlayed, int envidoScore, int handsPlayedCount,
                            boolean isMano, boolean canPlayCard, boolean canFold,
                            boolean foldWouldGiveGameToBot, int pointsToWin) {

  }

  public record TrucoContext(BotTrucoCall availableCall, List<BotTrucoResponse> availableResponses,
                             BotTrucoCall currentCall) {

    public boolean mustRespond() {

      return !this.availableResponses.isEmpty();
    }

    public boolean canCall() {

      return this.availableCall != null;
    }

    public boolean canRespondWith(final BotTrucoResponse answer) {

      return this.availableResponses.contains(answer);
    }

  }

  public record EnvidoContext(List<BotEnvidoCall> availableCalls,
                              List<BotEnvidoResponse> availableResponses,
                              List<BotEnvidoCall> currentChain,
                              PendingEnvidoOutcome pendingOutcome) {

    public boolean mustRespond() {

      return !this.availableResponses.isEmpty();
    }

    public boolean canCall() {

      return !this.availableCalls.isEmpty();
    }

  }

  public record PendingEnvidoOutcome(int acceptedPointsIfBotWins, int acceptedPointsIfRivalWins,
                                     int rejectedPoints) {

  }

}
