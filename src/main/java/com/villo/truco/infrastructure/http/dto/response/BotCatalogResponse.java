package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.BotCatalogDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Catálogo de bots disponibles para el jugador")
public record BotCatalogResponse(
    @Schema(description = "Bots del modo casual") List<BotProfileResponse> casual,
    @Schema(description = "Bots de campaña desbloqueados (historial neto ≥ 3 a favor)") List<BotProfileResponse> campaignUnlocked) {

  public static BotCatalogResponse from(final BotCatalogDTO dto) {

    return new BotCatalogResponse(dto.casual().stream().map(BotProfileResponse::from).toList(),
        dto.campaignUnlocked().stream().map(BotProfileResponse::from).toList());
  }

}
