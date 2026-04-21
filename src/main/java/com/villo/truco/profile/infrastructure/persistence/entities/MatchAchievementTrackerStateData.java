package com.villo.truco.profile.infrastructure.persistence.entities;

import java.util.List;

public record MatchAchievementTrackerStateData(int currentGameNumber, int currentRoundNumber,
                                               String manoSeat, int previousScorePlayerOne,
                                               int previousScorePlayerTwo, int scorePlayerOne,
                                               int scorePlayerTwo, int playedHandsInRound,
                                               boolean roundHadCalls,
                                               List<String> envidoCallsInRound,
                                               String lastEnvidoResponse,
                                               String lastEnvidoWinnerSeat,
                                               Integer lastEnvidoPointsMano,
                                               Integer lastEnvidoPointsPie,
                                               String lastTrucoResponse,
                                               String lastTrucoResponseCall,
                                               String lastTrucoResponderSeat,
                                               CardData lastHandCardPlayerOne,
                                               CardData lastHandCardPlayerTwo,
                                               String lastHandWinnerSeat, String lastFoldedSeat,
                                               String lastRoundWinnerSeat,
                                               boolean pendingAcceptedValeCuatro,
                                               boolean playerOneLostAcceptedValeCuatroByBust,
                                               boolean playerTwoLostAcceptedValeCuatroByBust) {

  public record CardData(String suit, Integer number) {

  }
}
