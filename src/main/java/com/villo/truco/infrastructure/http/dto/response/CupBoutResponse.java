package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.CupBoutDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Bout (cruce) de la copa")
public record CupBoutResponse(
    @Schema(description = "ID del bout", example = "bout-abc123") String boutId,
    @Schema(description = "Número de ronda", example = "1") int roundNumber,
    @Schema(description = "Posición en el bracket", example = "0") int bracketPosition,
    @Schema(description = "ID del jugador uno", example = "player-1") String playerOneId,
    @Schema(description = "ID del jugador dos", example = "player-2") String playerTwoId,
    @Schema(description = "ID del match asociado", example = "match-abc123") String matchId,
    @Schema(description = "ID del ganador", example = "player-1") String winnerId,
    @Schema(description = "Estado del bout", example = "PENDING") String status) {

  public static CupBoutResponse from(final CupBoutDTO dto) {

    return new CupBoutResponse(dto.boutId(), dto.roundNumber(), dto.bracketPosition(),
        dto.playerOneId(), dto.playerTwoId(), dto.matchId(), dto.winnerId(), dto.status());
  }

}
