package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.CreateLeagueDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta al crear liga")
public record CreateLeagueResponse(
    @Schema(description = "ID del liga", example = "league-123") String leagueId,
    @Schema(description = "Código de invitación del liga", example = "ABCD1234") String inviteCode) {

  public static CreateLeagueResponse from(final CreateLeagueDTO dto) {

    return new CreateLeagueResponse(dto.leagueId(), dto.inviteCode());
  }

}
