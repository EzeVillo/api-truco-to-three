package com.villo.truco.application.dto;

public record MatchStateDTO(String matchId, String status, String viewerSeat,
                            String playerOneUsername, String playerTwoUsername, int gamesToPlay,
                            int scorePlayerOne, int scorePlayerTwo, int gamesWonPlayerOne,
                            int gamesWonPlayerTwo, String matchWinner, LobbyStateDTO lobby,
                            RoundStateDTO currentRound, int spectatorCount, long stateVersion) {

}
