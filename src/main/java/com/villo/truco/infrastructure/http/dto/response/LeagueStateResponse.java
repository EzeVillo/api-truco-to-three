package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.LeagueStateDTO;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Estado completo de liga")
public record LeagueStateResponse(
    @Schema(description = "ID del liga", example = "league-123") String leagueId,
    @Schema(description = "Estado del liga", example = "IN_PROGRESS") String status,
    @ArraySchema(schema = @Schema(implementation = LeagueStandingResponse.class), arraySchema = @Schema(description = "Tabla de posiciones")) List<LeagueStandingResponse> standings,
    @ArraySchema(schema = @Schema(description = "ID de jugador ganador", example = "player-1"), arraySchema = @Schema(description = "Ganador(es) del liga")) List<String> winners,
    @ArraySchema(schema = @Schema(implementation = LeagueMatchdayResponse.class), arraySchema = @Schema(description = "Calendario por jornadas")) List<LeagueMatchdayResponse> matchdays) {

  public static LeagueStateResponse from(final LeagueStateDTO dto) {

    return new LeagueStateResponse(dto.leagueId(), dto.status(),
        dto.standings().stream().map(LeagueStandingResponse::from).toList(), dto.winners(),
        dto.matchdays().stream().map(LeagueMatchdayResponse::from).toList());
  }

}
