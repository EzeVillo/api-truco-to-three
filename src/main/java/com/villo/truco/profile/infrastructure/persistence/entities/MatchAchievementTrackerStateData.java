package com.villo.truco.profile.infrastructure.persistence.entities;

import java.util.List;

public record MatchAchievementTrackerStateData(int currentGameNumber, String manoSeat,
                                               int previousScorePlayerOne,
                                               int previousScorePlayerTwo, int scorePlayerOne,
                                               int scorePlayerTwo, int playedHandsInRound,
                                               int cardsPlayedInRound, boolean roundHadCalls,
                                               List<String> envidoCallsInRound,
                                               String lastEnvidoResponse,
                                               String lastEnvidoWinnerSeat,
                                               Integer lastEnvidoPointsMano,
                                               Integer lastEnvidoPointsPie,
                                               String lastTrucoResponse,
                                               String lastTrucoResponseCall,
                                               String lastTrucoResponderSeat,
                                               String lastTrucoCallerSeat,
                                               CardData lastHandCardPlayerOne,
                                               CardData lastHandCardPlayerTwo,
                                               String lastHandWinnerSeat, String lastFoldedSeat,
                                               boolean playerOnePlayedCurrentHand,
                                               boolean playerTwoPlayedCurrentHand,
                                               boolean trucoCalledWhenRivalHadNoCards) {

  public record CardData(String suit, Integer number) {

  }

}
