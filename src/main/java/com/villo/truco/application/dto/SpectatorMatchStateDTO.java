package com.villo.truco.application.dto;

public record SpectatorMatchStateDTO(String matchId, String status, int scorePlayerOne,
                                     int scorePlayerTwo, int gamesWonPlayerOne,
                                     int gamesWonPlayerTwo, String matchWinner,
                                     SpectatorRoundStateDTO currentRound, int spectatorCount) {

}
