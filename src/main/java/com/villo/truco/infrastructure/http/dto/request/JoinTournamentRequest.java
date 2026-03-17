package com.villo.truco.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Solicitud para unirse a un torneo")
public record JoinTournamentRequest(
    @Schema(description = "Código de invitación del torneo", example = "ABCD1234") String inviteCode) {

}
