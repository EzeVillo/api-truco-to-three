package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.MatchStateDTO;

public record MatchStateResponse(String matchId, String status, int gamesWonPlayerOne,
                                 int gamesWonPlayerTwo, String matchWinner,
                                 RoundStateResponse RoundGame) {

  public static MatchStateResponse from(final MatchStateDTO dto) {

    return new MatchStateResponse(dto.matchId(), dto.status(), dto.gamesWonPlayerOne(),
        dto.gamesWonPlayerTwo(), dto.matchWinner(),
        dto.currentRound() != null ? RoundStateResponse.from(dto.currentRound()) : null);
  }

}
