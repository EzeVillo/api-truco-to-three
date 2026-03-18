package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.LeagueMatchdayDTO;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Jornada de liga")
public record LeagueMatchdayResponse(
    @Schema(description = "Número de jornada", example = "1") int matchdayNumber,
    @ArraySchema(schema = @Schema(implementation = LeagueFixtureResponse.class), arraySchema = @Schema(description = "Partidos de la jornada")) List<LeagueFixtureResponse> fixtures) {

  public static LeagueMatchdayResponse from(final LeagueMatchdayDTO dto) {

    return new LeagueMatchdayResponse(dto.matchdayNumber(),
        dto.fixtures().stream().map(LeagueFixtureResponse::from).toList());
  }

}
