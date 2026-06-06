package com.villo.truco.application.dto;

public record SpectatorMatchStateDTO(String matchId, String status, String playerOneUsername,
                                     String playerTwoUsername, int scorePlayerOne,
                                     int scorePlayerTwo, int gamesWonPlayerOne,
                                     int gamesWonPlayerTwo, int gamesToPlay, String matchWinner,
                                     SpectatorRoundStateDTO currentRound, int spectatorCount,
                                     long stateVersion) {

}
