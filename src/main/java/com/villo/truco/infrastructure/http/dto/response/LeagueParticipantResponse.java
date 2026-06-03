package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.LeagueParticipantDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Participante de la sala de liga")
public record LeagueParticipantResponse(
    @Schema(description = "Nombre visible del jugador", example = "juancho") String player,
    @Schema(description = "Indica si el jugador es el creador de la sala", example = "true") boolean creator) {

  public static LeagueParticipantResponse from(final LeagueParticipantDTO dto) {

    return new LeagueParticipantResponse(dto.player(), dto.creator());
  }

}
