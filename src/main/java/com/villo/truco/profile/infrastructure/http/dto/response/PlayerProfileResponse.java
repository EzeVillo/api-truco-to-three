package com.villo.truco.profile.infrastructure.http.dto.response;

import com.villo.truco.profile.application.dto.PlayerProfileDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Perfil completo del jugador")
public record PlayerProfileResponse(
    @Schema(description = "Nombre de usuario", example = "pepito") String username,
    @Schema(description = "Logros desbloqueados") List<UnlockedAchievementResponse> achievements,
    @Schema(description = "Estadísticas de partidas") PlayerStatsResponse stats) {

  public static PlayerProfileResponse from(final PlayerProfileDTO dto) {

    final var achievements = dto.achievements().stream().map(UnlockedAchievementResponse::from)
        .toList();
    return new PlayerProfileResponse(dto.username(), achievements,
        PlayerStatsResponse.from(dto.stats()));
  }

}
