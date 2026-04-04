package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.PublicMatchLobbyDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Sala publica de partida visible en el lobby")
public record PublicMatchLobbyResponse(
    @Schema(description = "ID de la partida", example = "550e8400-e29b-41d4-a716-446655440000") String matchId,
    @Schema(description = "Host de la sala", example = "juancho") String host,
    @Schema(description = "Cantidad de partidas de la serie", example = "3") int gamesToPlay,
    @Schema(description = "Cupo total de jugadores", example = "2") int totalSlots,
    @Schema(description = "Cupo actualmente ocupado", example = "1") int occupiedSlots,
    @Schema(description = "Estado actual", example = "WAITING_FOR_PLAYERS") String status,
    @Schema(description = "Links accionables del lobby") PublicLobbyItemLinksResponse _links) {

  public static PublicMatchLobbyResponse from(final PublicMatchLobbyDTO dto) {

    return new PublicMatchLobbyResponse(dto.matchId(), dto.host(), dto.gamesToPlay(),
        dto.totalSlots(), dto.occupiedSlots(), dto.status(),
        PublicLobbyLinkFactory.itemLinks("/api/matches", dto.matchId()));
  }

}
