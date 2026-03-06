package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.JoinMatchDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta al unirse a partida")
public record JoinMatchResponse(
    @Schema(description = "JWT Bearer del jugador que se unió") String accessToken) {

  public static JoinMatchResponse from(final JoinMatchDTO dto) {

    return new JoinMatchResponse(dto.accessToken());
  }

}
