package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.CreateBotVsBotMatchDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta al crear partida entre dos bots")
public record CreateBotVsBotMatchResponse(
    @Schema(description = "ID de la partida creada", example = "match-123") String matchId) {

  public static CreateBotVsBotMatchResponse from(final CreateBotVsBotMatchDTO dto) {

    return new CreateBotVsBotMatchResponse(dto.matchId());
  }

}
