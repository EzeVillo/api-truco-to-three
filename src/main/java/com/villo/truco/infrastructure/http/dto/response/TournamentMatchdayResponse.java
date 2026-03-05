package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.TournamentMatchdayDTO;
import java.util.List;

public record TournamentMatchdayResponse(int matchdayNumber,
                                         List<TournamentFixtureResponse> fixtures) {

  public static TournamentMatchdayResponse from(final TournamentMatchdayDTO dto) {

    return new TournamentMatchdayResponse(dto.matchdayNumber(),
        dto.fixtures().stream().map(TournamentFixtureResponse::from).toList());
  }

}
