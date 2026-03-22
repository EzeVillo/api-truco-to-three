package com.villo.truco.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Solicitud para unirse a una copa")
public record JoinCupRequest(
    @NotBlank @Schema(description = "Código de invitación de la copa", example = "ABCD1234") String inviteCode) {

}
