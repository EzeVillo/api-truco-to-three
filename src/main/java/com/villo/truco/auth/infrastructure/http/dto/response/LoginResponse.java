package com.villo.truco.auth.infrastructure.http.dto.response;

import com.villo.truco.auth.application.model.UserAuthenticatedSession;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta al hacer login")
public record LoginResponse(
    @Schema(description = "ID del jugador", example = "550e8400-e29b-41d4-a716-446655440000") String playerId,
    @Schema(description = "JWT Bearer para endpoints protegidos", example = "eyJhbGciOi...") String accessToken,
    @Schema(description = "Refresh token opaco", example = "7H5Q3v2...") String refreshToken,
    @Schema(description = "Segundos hasta expiracion del access token", example = "900") long accessTokenExpiresIn,
    @Schema(description = "Segundos hasta expiracion del refresh token", example = "2592000") long refreshTokenExpiresIn) {

  public static LoginResponse from(final UserAuthenticatedSession session) {

    return new LoginResponse(session.playerId().value().toString(), session.accessToken(),
        session.refreshToken(), session.accessTokenExpiresIn(), session.refreshTokenExpiresIn());
  }

}
