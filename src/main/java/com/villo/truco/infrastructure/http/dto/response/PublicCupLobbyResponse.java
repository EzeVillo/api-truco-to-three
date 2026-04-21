package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.PublicCupLobbyDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Sala publica de copa visible en el lobby")
public record PublicCupLobbyResponse(
    @Schema(description = "ID de la copa", example = "550e8400-e29b-41d4-a716-446655440000") String cupId,
    @Schema(description = "Creador de la sala", example = "juancho") String host,
    @Schema(description = "Cantidad de partidas por cruce", example = "3") int gamesToPlay,
    @Schema(description = "Cupo total de jugadores", example = "8") int totalSlots,
    @Schema(description = "Cupo actualmente ocupado", example = "5") int occupiedSlots,
    @Schema(description = "Estado actual", example = "WAITING_FOR_PLAYERS") String status,
    @Schema(description = "Join code compartible de la copa", example = "ABCD1234") String joinCode,
    @Schema(description = "Links accionables del lobby") PublicLobbyItemLinksResponse _links) {

  public static PublicCupLobbyResponse from(final PublicCupLobbyDTO dto) {

    return new PublicCupLobbyResponse(dto.cupId(), dto.host(), dto.gamesToPlay(), dto.totalSlots(),
        dto.occupiedSlots(), dto.status(), dto.joinCode(),
        PublicLobbyLinkFactory.itemLinks(dto.joinCode()));
  }

}
