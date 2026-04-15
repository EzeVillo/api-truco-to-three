package com.villo.truco.social.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Solicitud de invitación a un recurso")
public record CreateResourceInvitationRequest(
    @NotBlank @Schema(description = "Nombre de usuario del jugador destinatario", example = "martina") String recipientUsername,
    @NotBlank @Schema(description = "Tipo de recurso: MATCH, LEAGUE o CUP", example = "MATCH") String targetType,
    @NotBlank @Schema(description = "ID del recurso al que se invita", example = "00000000-0000-0000-0000-000000000002") String targetId) {

}
