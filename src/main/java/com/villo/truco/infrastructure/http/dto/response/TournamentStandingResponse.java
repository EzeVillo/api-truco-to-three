package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.TournamentStandingDTO;

public record TournamentStandingResponse(String playerId, int wins) {

  public static TournamentStandingResponse from(final TournamentStandingDTO dto) {

    return new TournamentStandingResponse(dto.playerId(), dto.wins());
  }

}
