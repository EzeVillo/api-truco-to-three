package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.LeagueStateDTO;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Estado completo de liga")
public record LeagueStateResponse(
    @Schema(description = "ID del liga", example = "league-123") String leagueId,
    @Schema(description = "Estado del liga", example = "IN_PROGRESS") String status,
    @Schema(description = "Creador de la sala", example = "juancho") String host,
    @Schema(description = "Cupo total de jugadores", example = "4") int totalSlots,
    @Schema(description = "Cupo actualmente ocupado", example = "2") int occupiedSlots,
    @Schema(description = "Indica si el jugador autenticado puede iniciar la liga", example = "false") boolean canStart,
    @ArraySchema(schema = @Schema(implementation = LeagueParticipantResponse.class), arraySchema = @Schema(description = "Participantes actuales de la sala")) List<LeagueParticipantResponse> participants,
    @ArraySchema(schema = @Schema(implementation = LeagueStandingResponse.class), arraySchema = @Schema(description = "Tabla de posiciones")) List<LeagueStandingResponse> standings,
    @ArraySchema(schema = @Schema(description = "Nombre visible del jugador ganador", example = "juancho"), arraySchema = @Schema(description = "Ganador(es) del liga")) List<String> winners,
    @ArraySchema(schema = @Schema(implementation = LeagueMatchdayResponse.class), arraySchema = @Schema(description = "Calendario por jornadas")) List<LeagueMatchdayResponse> matchdays,
    @Schema(description = "Visibilidad de la liga", example = "PRIVATE", allowableValues = {
        "PUBLIC", "PRIVATE"}) String visibility,
    @Schema(description = "Codigo para invitar/unirse a la liga", example = "ABCD2345") String joinCode,
    @Schema(description = "Epoch millis en que la sala expira por inactividad; null fuera de la espera", example = "1718246400000", nullable = true) Long lobbyTimeoutDeadline) {

  public static LeagueStateResponse from(final LeagueStateDTO dto) {

    return new LeagueStateResponse(dto.leagueId(), dto.status(), dto.host(), dto.totalSlots(),
        dto.occupiedSlots(), dto.canStart(),
        dto.participants().stream().map(LeagueParticipantResponse::from).toList(),
        dto.standings().stream().map(LeagueStandingResponse::from).toList(), dto.winners(),
        dto.matchdays().stream().map(LeagueMatchdayResponse::from).toList(), dto.visibility(),
        dto.joinCode(), dto.lobbyTimeoutDeadline());
  }

}
