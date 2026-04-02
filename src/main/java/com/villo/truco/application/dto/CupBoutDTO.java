package com.villo.truco.application.dto;

public record CupBoutDTO(String boutId, int roundNumber, int bracketPosition, String playerOne,
                         String playerTwo, String matchId, String winner, String status) {

}
