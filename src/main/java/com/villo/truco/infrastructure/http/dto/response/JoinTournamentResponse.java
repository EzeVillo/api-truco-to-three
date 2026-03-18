package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.JoinTournamentDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta al unirse a un torneo")
public record JoinTournamentResponse(
    @Schema(description = "ID del torneo al que se unió") String tournamentId) {

  public static JoinTournamentResponse from(final JoinTournamentDTO dto) {

    return new JoinTournamentResponse(dto.tournamentId());
  }

}
