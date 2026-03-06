package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.CreateTournamentDTO;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Respuesta al crear torneo")
public record CreateTournamentResponse(
    @Schema(description = "ID del torneo", example = "tournament-123") String tournamentId,
    @ArraySchema(schema = @Schema(implementation = TournamentMatchdayResponse.class), arraySchema = @Schema(description = "Calendario inicial por jornadas")) List<TournamentMatchdayResponse> matchdays) {

  public static CreateTournamentResponse from(final CreateTournamentDTO dto) {

    return new CreateTournamentResponse(dto.tournamentId(),
        dto.matchdays().stream().map(TournamentMatchdayResponse::from).toList());
  }

}
