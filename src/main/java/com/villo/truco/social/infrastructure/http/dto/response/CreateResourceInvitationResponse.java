package com.villo.truco.social.infrastructure.http.dto.response;

import com.villo.truco.social.application.dto.ResourceInvitationDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta al crear una invitación a un recurso")
public record CreateResourceInvitationResponse(
    @Schema(description = "ID de la invitación generada", example = "00000000-0000-0000-0000-000000000001") String invitationId,
    @Schema(description = "Timestamp de expiración en milisegundos epoch", example = "1775000000000") long expiresAt) {

  public static CreateResourceInvitationResponse from(final ResourceInvitationDTO dto) {

    return new CreateResourceInvitationResponse(dto.invitationId(), dto.expiresAt());
  }

}
