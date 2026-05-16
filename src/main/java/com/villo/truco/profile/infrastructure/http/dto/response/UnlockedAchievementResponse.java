package com.villo.truco.profile.infrastructure.http.dto.response;

import com.villo.truco.profile.application.dto.UnlockedAchievementDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Logro desbloqueado por el jugador")
public record UnlockedAchievementResponse(
    @Schema(description = "Código del logro", example = "WIN_MATCH_FIRST") String achievementCode,
    @Schema(description = "Timestamp de desbloqueo en milisegundos epoch", example = "1747353600000") long unlockedAt,
    @Schema(description = "ID de la partida donde se desbloqueó", example = "550e8400-e29b-41d4-a716-446655440001") UUID matchId,
    @Schema(description = "Número de juego dentro de la partida", example = "2") int gameNumber) {

  public static UnlockedAchievementResponse from(final UnlockedAchievementDTO dto) {

    return new UnlockedAchievementResponse(dto.achievementCode().name(),
        dto.unlockedAt().toEpochMilli(), dto.matchId(), dto.gameNumber());
  }

}
