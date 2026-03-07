package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.MatchStateDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado de una partida")
public record MatchStateResponse(
    @Schema(description = "ID de la partida", example = "match-123") String matchId,
    @Schema(description = "Estado de la partida", example = "IN_PROGRESS") String status,
    @Schema(description = "Juegos ganados por player one", example = "1") int gamesWonPlayerOne,
    @Schema(description = "Juegos ganados por player two", example = "0") int gamesWonPlayerTwo,
    @Schema(description = "ID del ganador final de la partida, si existe", example = "player-1") String matchWinner,
    @Schema(description = "Estado de la ronda actual") RoundStateResponse roundGame) {

  public static MatchStateResponse from(final MatchStateDTO dto) {

    return new MatchStateResponse(dto.matchId(), dto.status(), dto.gamesWonPlayerOne(),
        dto.gamesWonPlayerTwo(), dto.matchWinner(),
        dto.currentRound() != null ? RoundStateResponse.from(dto.currentRound()) : null);
  }

}
