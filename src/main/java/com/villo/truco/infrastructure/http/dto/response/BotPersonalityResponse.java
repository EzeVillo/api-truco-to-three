package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.BotPersonalityDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Parámetros de personalidad del bot (1-100)")
public record BotPersonalityResponse(
    @Schema(description = "Tendencia a bluffear con mano débil") int mentiroso,
    @Schema(description = "Espera que el rival cante envido para subir la apuesta") int pescador,
    @Schema(description = "Agresividad para escalar cantos de truco") int temerario,
    @Schema(description = "Agresividad con el envido") int envidoso,
    @Schema(description = "Guarda cartas fuertes para manos posteriores") int aguantador) {

  public static BotPersonalityResponse from(final BotPersonalityDTO dto) {

    return new BotPersonalityResponse(dto.mentiroso(), dto.pescador(), dto.temerario(),
        dto.envidoso(), dto.aguantador());
  }

}
