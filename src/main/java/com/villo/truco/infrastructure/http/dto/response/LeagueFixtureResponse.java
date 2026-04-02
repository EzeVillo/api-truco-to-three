package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.LeagueFixtureDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fixture de liga")
public record LeagueFixtureResponse(
    @Schema(description = "ID del fixture", example = "fixture-123") String fixtureId,
    @Schema(description = "Numero de jornada", example = "1") int matchdayNumber,
    @Schema(description = "Nombre visible del jugador local", example = "juancho") String playerOne,
    @Schema(description = "Nombre visible del jugador visitante", example = "martina") String playerTwo,
    @Schema(description = "ID de partida asociada", example = "match-123") String matchId,
    @Schema(description = "Nombre visible del ganador si ya termino", example = "juancho") String winner,
    @Schema(description = "Estado del fixture", example = "SCHEDULED") String status) {

  public static LeagueFixtureResponse from(final LeagueFixtureDTO dto) {

    return new LeagueFixtureResponse(dto.fixtureId(), dto.matchdayNumber(), dto.playerOne(),
        dto.playerTwo(), dto.matchId(), dto.winner(), dto.status());
  }

}
