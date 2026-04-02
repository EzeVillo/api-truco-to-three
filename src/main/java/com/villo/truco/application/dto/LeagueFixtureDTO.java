package com.villo.truco.application.dto;

public record LeagueFixtureDTO(String fixtureId, int matchdayNumber, String playerOne,
                               String playerTwo, String matchId, String winner, String status) {

}
