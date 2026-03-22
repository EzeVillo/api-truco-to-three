package com.villo.truco.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Solicitud para unirse a una partida")
public record JoinMatchRequest(
    @NotBlank @Schema(description = "Código de invitación de la partida", example = "ABC123") String inviteCode) {

}
