package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.CreateLeagueDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta al crear liga")
public record CreateLeagueResponse(
    @Schema(description = "ID de la liga", example = "league-123") String leagueId,
    @Schema(description = "Codigo compartible para unirse a la liga", example = "ABCD1234") String joinCode,
    @Schema(description = "Visibilidad de la sala", example = "PRIVATE") String visibility) {

  public static CreateLeagueResponse from(final CreateLeagueDTO dto) {

    return new CreateLeagueResponse(dto.leagueId(), dto.joinCode(), dto.visibility());
  }

}
