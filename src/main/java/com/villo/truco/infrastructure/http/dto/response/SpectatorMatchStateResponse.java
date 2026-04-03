package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.SpectatorMatchStateDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado de una partida para espectador")
public record SpectatorMatchStateResponse(
    @Schema(description = "ID de la partida", example = "match-123") String matchId,
    @Schema(description = "Estado de la partida", example = "IN_PROGRESS") String status,
    @Schema(description = "Juegos ganados por player one", example = "1") int gamesWonPlayerOne,
    @Schema(description = "Juegos ganados por player two", example = "0") int gamesWonPlayerTwo,
    @Schema(description = "ID del ganador final, si existe", example = "juancho") String matchWinner,
    @Schema(description = "Estado de la ronda actual") SpectatorRoundStateResponse currentRound,
    @Schema(description = "Cantidad de espectadores", example = "3") int spectatorCount) {

  public static SpectatorMatchStateResponse from(final SpectatorMatchStateDTO dto) {

    return new SpectatorMatchStateResponse(dto.matchId(), dto.status(), dto.gamesWonPlayerOne(),
        dto.gamesWonPlayerTwo(), dto.matchWinner(),
        dto.currentRound() != null ? SpectatorRoundStateResponse.from(dto.currentRound()) : null,
        dto.spectatorCount());
  }

}
