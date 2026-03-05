package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.CreateTournamentDTO;
import java.util.List;

public record CreateTournamentResponse(String tournamentId,
                                       List<TournamentMatchdayResponse> matchdays) {

  public static CreateTournamentResponse from(final CreateTournamentDTO dto) {

    return new CreateTournamentResponse(dto.tournamentId(),
        dto.matchdays().stream().map(TournamentMatchdayResponse::from).toList());
  }

}
