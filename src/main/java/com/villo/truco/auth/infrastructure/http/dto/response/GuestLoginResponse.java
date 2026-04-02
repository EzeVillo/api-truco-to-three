package com.villo.truco.auth.infrastructure.http.dto.response;

import com.villo.truco.auth.application.model.GuestAuthenticatedSession;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta al acceder como invitado")
public record GuestLoginResponse(
    @Schema(description = "ID efimero del jugador invitado", example = "550e8400-e29b-41d4-a716-446655440000") String playerId,
    @Schema(description = "JWT Bearer para endpoints protegidos", example = "eyJhbGciOi...") String accessToken,
    @Schema(description = "Segundos hasta expiracion del access token", example = "604800") long accessTokenExpiresIn) {

  public static GuestLoginResponse from(final GuestAuthenticatedSession session) {

    return new GuestLoginResponse(session.playerId().value().toString(), session.accessToken(),
        session.accessTokenExpiresIn());
  }

}
