package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.LeagueStandingDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fila de la tabla de posiciones")
public record LeagueStandingResponse(
    @Schema(description = "Nombre visible del jugador", example = "juancho") String player,
    @Schema(description = "Cantidad de victorias", example = "3") int wins) {

  public static LeagueStandingResponse from(final LeagueStandingDTO dto) {

    return new LeagueStandingResponse(dto.player(), dto.wins());
  }

}
