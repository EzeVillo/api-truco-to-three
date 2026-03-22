package com.villo.truco.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Solicitud para unirse a un liga")
public record JoinLeagueRequest(
    @NotBlank @Schema(description = "Código de invitación del liga", example = "ABCD1234") String inviteCode) {

}
