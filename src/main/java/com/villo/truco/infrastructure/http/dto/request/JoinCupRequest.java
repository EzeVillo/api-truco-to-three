package com.villo.truco.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Solicitud para unirse a una copa")
public record JoinCupRequest(
    @Schema(description = "Código de invitación de la copa", example = "ABCD1234") String inviteCode) {

}
