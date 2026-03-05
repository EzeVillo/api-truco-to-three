package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.TournamentFixtureDTO;

public record TournamentFixtureResponse(String fixtureId, int matchdayNumber, String playerOneId,
                                        String playerTwoId, String matchId, String winnerPlayerId,
                                        String status) {

  public static TournamentFixtureResponse from(final TournamentFixtureDTO dto) {

    return new TournamentFixtureResponse(dto.fixtureId(), dto.matchdayNumber(), dto.playerOneId(),
        dto.playerTwoId(), dto.matchId(), dto.winnerPlayerId(), dto.status());
  }

}
