package com.villo.truco.application.dto;

public record MatchStateDTO(String matchId, String status, int scorePlayerOne, int scorePlayerTwo,
                            int gamesWonPlayerOne, int gamesWonPlayerTwo, String matchWinner,
                            RoundStateDTO currentRound) {

}
