package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.CreateTournamentDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta al crear torneo")
public record CreateTournamentResponse(
    @Schema(description = "ID del torneo", example = "tournament-123") String tournamentId,
    @Schema(description = "Código de invitación del torneo", example = "ABCD1234") String inviteCode) {

  public static CreateTournamentResponse from(final CreateTournamentDTO dto) {

    return new CreateTournamentResponse(dto.tournamentId(), dto.inviteCode());
  }

}
