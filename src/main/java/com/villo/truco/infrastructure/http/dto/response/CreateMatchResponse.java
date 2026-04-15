package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.CreateMatchDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta al crear partida")
public record CreateMatchResponse(
    @Schema(description = "ID de la partida creada", example = "match-123") String matchId,
    @Schema(description = "Codigo compartible para unirse a la partida", example = "ABC12345") String joinCode,
    @Schema(description = "Visibilidad de la sala", example = "PRIVATE") String visibility) {

  public static CreateMatchResponse from(final CreateMatchDTO dto) {

    return new CreateMatchResponse(dto.matchId(), dto.joinCode(), dto.visibility());
  }

}
