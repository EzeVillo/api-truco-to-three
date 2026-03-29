package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.BotProfileDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Perfil de un bot")
public record BotProfileResponse(
    @Schema(description = "ID del bot", example = "00000000-0000-0000-0000-000000000001") String botId,
    @Schema(description = "Nombre del bot", example = "El Mentiroso") String name,
    @Schema(description = "Personalidad del bot") BotPersonalityResponse personality) {

  public static BotProfileResponse from(final BotProfileDTO dto) {

    return new BotProfileResponse(dto.botId(), dto.name(),
        BotPersonalityResponse.from(dto.personality()));
  }

}
