package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.TournamentStandingDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fila de la tabla de posiciones")
public record TournamentStandingResponse(
    @Schema(description = "ID de jugador", example = "player-1") String playerId,
    @Schema(description = "Cantidad de victorias", example = "3") int wins) {

  public static TournamentStandingResponse from(final TournamentStandingDTO dto) {

    return new TournamentStandingResponse(dto.playerId(), dto.wins());
  }

}
