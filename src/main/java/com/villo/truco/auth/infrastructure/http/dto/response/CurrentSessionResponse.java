package com.villo.truco.auth.infrastructure.http.dto.response;

import com.villo.truco.auth.application.model.AuthenticatedSessionIdentity;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Identidad de la sesion autenticada actual")
public record CurrentSessionResponse(
    @Schema(description = "ID del jugador autenticado", example = "550e8400-e29b-41d4-a716-446655440000") String playerId,
    @Schema(description = "Username del usuario registrado; null para guest", example = "juancho", nullable = true) String username,
    @Schema(description = "Tipo de token autenticado", example = "user") String tokenUse) {

  public static CurrentSessionResponse from(final AuthenticatedSessionIdentity identity) {

    return new CurrentSessionResponse(identity.playerId().value().toString(), identity.username(),
        identity.tokenUse());
  }

}
