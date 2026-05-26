package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.MatchStateDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado de una partida")
public record MatchStateResponse(
    @Schema(description = "ID de la partida", example = "match-123") String matchId,
    @Schema(description = "Estado de la partida", example = "IN_PROGRESS") String status,
    @Schema(description = "Asiento del jugador autenticado", example = "PLAYER_ONE", allowableValues = {
        "PLAYER_ONE", "PLAYER_TWO"}) String viewerSeat,
    @Schema(description = "Username del jugador en el asiento PLAYER_ONE", example = "juancho") String playerOneUsername,
    @Schema(description = "Username del jugador en el asiento PLAYER_TWO", example = "martina") String playerTwoUsername,
    @Schema(description = "Partidas totales de la serie (mejor de N)", example = "3", allowableValues = {
        "1", "3", "5"}) int gamesToPlay,
    @Schema(description = "Puntaje del game actual de player one", example = "2") int scorePlayerOne,
    @Schema(description = "Puntaje del game actual de player two", example = "1") int scorePlayerTwo,
    @Schema(description = "Juegos ganados por player one", example = "1") int gamesWonPlayerOne,
    @Schema(description = "Juegos ganados por player two", example = "0") int gamesWonPlayerTwo,
    @Schema(description = "Nombre visible del ganador final de la partida, si existe", example = "juancho") String matchWinner,
    @Schema(description = "Estado de la ronda actual") RoundStateResponse roundGame) {

  public static MatchStateResponse from(final MatchStateDTO dto) {

    return new MatchStateResponse(dto.matchId(), dto.status(), dto.viewerSeat(),
        dto.playerOneUsername(), dto.playerTwoUsername(), dto.gamesToPlay(), dto.scorePlayerOne(),
        dto.scorePlayerTwo(), dto.gamesWonPlayerOne(), dto.gamesWonPlayerTwo(), dto.matchWinner(),
        dto.currentRound() != null ? RoundStateResponse.from(dto.currentRound()) : null);
  }

}
