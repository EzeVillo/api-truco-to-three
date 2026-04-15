package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.PublicLeagueLobbyDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Sala publica de liga visible en el lobby")
public record PublicLeagueLobbyResponse(
    @Schema(description = "ID de la liga", example = "550e8400-e29b-41d4-a716-446655440000") String leagueId,
    @Schema(description = "Creador de la sala", example = "juancho") String host,
    @Schema(description = "Cantidad de partidas por fixture", example = "3") int gamesToPlay,
    @Schema(description = "Cupo total de jugadores", example = "4") int totalSlots,
    @Schema(description = "Cupo actualmente ocupado", example = "2") int occupiedSlots,
    @Schema(description = "Estado actual", example = "WAITING_FOR_PLAYERS") String status,
    @Schema(description = "Join code compartible de la liga", example = "ABCD1234") String joinCode,
    @Schema(description = "Links accionables del lobby") PublicLobbyItemLinksResponse _links) {

  public static PublicLeagueLobbyResponse from(final PublicLeagueLobbyDTO dto) {

    return new PublicLeagueLobbyResponse(dto.leagueId(), dto.host(), dto.gamesToPlay(),
        dto.totalSlots(), dto.occupiedSlots(), dto.status(), dto.joinCode(),
        PublicLobbyLinkFactory.itemLinks(dto.joinCode()));
  }

}
