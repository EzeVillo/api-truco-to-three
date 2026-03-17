package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.GuestLoginDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta al acceder como invitado")
public record GuestLoginResponse(
    @Schema(description = "ID efímero del jugador invitado") String playerId,
    @Schema(description = "JWT Bearer para endpoints protegidos") String accessToken) {

  public static GuestLoginResponse from(final GuestLoginDTO dto) {

    return new GuestLoginResponse(dto.playerId(), dto.accessToken());
  }

}
