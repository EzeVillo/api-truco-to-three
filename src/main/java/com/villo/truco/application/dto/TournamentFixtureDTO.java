package com.villo.truco.application.dto;

public record TournamentFixtureDTO(String fixtureId, int matchdayNumber, String playerOneId,
                                   String playerTwoId, String matchId, String winnerPlayerId,
                                   String status) {

}
