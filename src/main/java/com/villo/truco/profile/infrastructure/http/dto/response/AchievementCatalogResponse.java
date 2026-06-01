package com.villo.truco.profile.infrastructure.http.dto.response;

import com.villo.truco.profile.application.dto.AchievementCatalogDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Catálogo completo de logros existentes en el juego")
public record AchievementCatalogResponse(
    @Schema(description = "Lista de todos los logros existentes") List<AchievementCatalogItemResponse> achievements) {

  public static AchievementCatalogResponse from(final AchievementCatalogDTO dto) {

    final var items = dto.achievements().stream()
        .map(code -> new AchievementCatalogItemResponse(code.name())).toList();
    return new AchievementCatalogResponse(items);
  }

  @Schema(description = "Logro existente en el catálogo")
  public record AchievementCatalogItemResponse(
      @Schema(description = "Código del logro", example = "WIN_GAME_THREE_ZERO_VIA_ACCEPTED_RETRUCO") String achievementCode) {

  }

}
