package com.villo.truco.profile.infrastructure.http.dto.response;

import com.villo.truco.profile.application.dto.PlayerStatsDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estadísticas de partidas del jugador")
public record PlayerStatsResponse(
    @Schema(description = "Total de partidas jugadas", example = "42") int matchesPlayed,
    @Schema(description = "Partidas ganadas", example = "27") int matchesWon,
    @Schema(description = "Partidas perdidas", example = "15") int matchesLost,
    @Schema(description = "Tasa de victorias (0.0 - 1.0)", example = "0.64") double winRate) {

  public static PlayerStatsResponse from(final PlayerStatsDTO dto) {

    return new PlayerStatsResponse(dto.matchesPlayed(), dto.matchesWon(), dto.matchesLost(),
        dto.winRate());
  }

}
