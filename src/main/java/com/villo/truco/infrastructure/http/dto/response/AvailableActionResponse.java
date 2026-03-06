package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.AvailableActionDTO;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Acción disponible para el jugador")
public record AvailableActionResponse(
    @Schema(description = "Tipo de acción", example = "PLAY_CARD") String type,
    @ArraySchema(schema = @Schema(description = "Parámetro de la acción", example = "ESPADA:1"), arraySchema = @Schema(description = "Parámetros opcionales requeridos por la acción")) List<String> parameters) {

  public static AvailableActionResponse from(final AvailableActionDTO dto) {

    return new AvailableActionResponse(dto.type(), dto.parameters());
  }

}
