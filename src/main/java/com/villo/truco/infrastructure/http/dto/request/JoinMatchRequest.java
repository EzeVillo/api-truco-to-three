package com.villo.truco.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Solicitud para unirse a una partida")
public record JoinMatchRequest(
    @Schema(description = "Código de invitación de la partida", example = "ABC123") String inviteCode) {

  public JoinMatchRequest {

    Objects.requireNonNull(inviteCode, "InviteCode is required");
  }

}
