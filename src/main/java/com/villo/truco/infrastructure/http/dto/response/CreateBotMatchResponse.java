package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.CreateBotMatchDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta al crear partida contra bot")
public record CreateBotMatchResponse(
    @Schema(description = "ID de la partida creada", example = "match-123") String matchId) {

  public static CreateBotMatchResponse from(final CreateBotMatchDTO dto) {

    return new CreateBotMatchResponse(dto.matchId());
  }

}
