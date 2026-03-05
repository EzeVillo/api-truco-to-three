package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.TournamentStateDTO;
import java.util.List;

public record TournamentStateResponse(String tournamentId, String status,
                                      List<TournamentStandingResponse> standings,
                                      List<String> winners,
                                      List<TournamentMatchdayResponse> matchdays) {

  public static TournamentStateResponse from(final TournamentStateDTO dto) {

    return new TournamentStateResponse(dto.tournamentId(), dto.status(),
        dto.standings().stream().map(TournamentStandingResponse::from).toList(), dto.winners(),
        dto.matchdays().stream().map(TournamentMatchdayResponse::from).toList());
  }

}
