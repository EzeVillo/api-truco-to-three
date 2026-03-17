package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.LoginDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta al hacer login")
public record LoginResponse(@Schema(description = "ID del jugador") String playerId,
                            @Schema(description = "JWT Bearer para endpoints protegidos") String accessToken) {

  public static LoginResponse from(final LoginDTO dto) {

    return new LoginResponse(dto.playerId(), dto.accessToken());
  }

}
