package com.villo.truco.application.dto;

public record CupBoutDTO(String boutId, int roundNumber, int bracketPosition, String playerOneId,
                         String playerTwoId, String matchId, String winnerId, String status) {

}
