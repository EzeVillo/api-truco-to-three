package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.RegisterUserDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta al registrar usuario")
public record RegisterUserResponse(
    @Schema(description = "ID del jugador registrado", example = "player-abc123") String playerId,
    @Schema(description = "JWT Bearer para endpoints protegidos", example = "eyJhbGciOi...") String accessToken) {

  public static RegisterUserResponse from(final RegisterUserDTO dto) {

    return new RegisterUserResponse(dto.playerId(), dto.accessToken());
  }

}
