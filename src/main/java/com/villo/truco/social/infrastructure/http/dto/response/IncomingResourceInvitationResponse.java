package com.villo.truco.social.infrastructure.http.dto.response;

import com.villo.truco.social.application.dto.ResourceInvitationDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos de una invitacion a un recurso recibida")
public record IncomingResourceInvitationResponse(
    @Schema(description = "ID de la invitacion", example = "00000000-0000-0000-0000-000000000001") String invitationId,
    @Schema(description = "Nombre de usuario del jugador que envio la invitacion", example = "juancho") String senderUsername,
    @Schema(description = "Tipo de recurso: MATCH, LEAGUE o CUP", example = "MATCH") String targetType,
    @Schema(description = "ID del recurso al que se invita", example = "00000000-0000-0000-0000-000000000004") String targetId,
    @Schema(description = "Estado de la invitacion: PENDING, ACCEPTED, DECLINED o EXPIRED", example = "PENDING") String status,
    @Schema(description = "Timestamp de expiracion en milisegundos epoch", example = "1775000000000") long expiresAt) {

  public static IncomingResourceInvitationResponse from(final ResourceInvitationDTO dto) {

    return new IncomingResourceInvitationResponse(dto.invitationId(), dto.senderUsername(),
        dto.targetType(), dto.targetId(), dto.status(), dto.expiresAt());
  }

}
