package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.TournamentMatchdayDTO;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Jornada de torneo")
public record TournamentMatchdayResponse(
    @Schema(description = "Número de jornada", example = "1") int matchdayNumber,
    @ArraySchema(schema = @Schema(implementation = TournamentFixtureResponse.class), arraySchema = @Schema(description = "Partidos de la jornada")) List<TournamentFixtureResponse> fixtures) {

  public static TournamentMatchdayResponse from(final TournamentMatchdayDTO dto) {

    return new TournamentMatchdayResponse(dto.matchdayNumber(),
        dto.fixtures().stream().map(TournamentFixtureResponse::from).toList());
  }

}
