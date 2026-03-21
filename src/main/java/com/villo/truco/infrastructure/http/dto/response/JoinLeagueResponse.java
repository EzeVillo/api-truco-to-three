package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.JoinLeagueDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta al unirse a un liga")
public record JoinLeagueResponse(
    @Schema(description = "ID del liga al que se unió", example = "league-abc123") String leagueId) {

  public static JoinLeagueResponse from(final JoinLeagueDTO dto) {

    return new JoinLeagueResponse(dto.leagueId());
  }

}
