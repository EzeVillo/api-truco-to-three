package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.TournamentStateDTO;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Estado completo de torneo")
public record TournamentStateResponse(
    @Schema(description = "ID del torneo", example = "tournament-123") String tournamentId,
    @Schema(description = "Estado del torneo", example = "IN_PROGRESS") String status,
    @ArraySchema(schema = @Schema(implementation = TournamentStandingResponse.class), arraySchema = @Schema(description = "Tabla de posiciones")) List<TournamentStandingResponse> standings,
    @ArraySchema(schema = @Schema(description = "ID de jugador ganador", example = "player-1"), arraySchema = @Schema(description = "Ganador(es) del torneo")) List<String> winners,
    @ArraySchema(schema = @Schema(implementation = TournamentMatchdayResponse.class), arraySchema = @Schema(description = "Calendario por jornadas")) List<TournamentMatchdayResponse> matchdays) {

  public static TournamentStateResponse from(final TournamentStateDTO dto) {

    return new TournamentStateResponse(dto.tournamentId(), dto.status(),
        dto.standings().stream().map(TournamentStandingResponse::from).toList(), dto.winners(),
        dto.matchdays().stream().map(TournamentMatchdayResponse::from).toList());
  }

}
