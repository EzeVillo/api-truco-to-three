package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.JoinResourceDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado de un join resuelto por joinCode")
public record JoinResourceResponse(
    @Schema(description = "Tipo de recurso unido", example = "MATCH") String targetType,
    @Schema(description = "ID del recurso unido", example = "match-123") String targetId) {

  public static JoinResourceResponse from(final JoinResourceDTO dto) {

    return new JoinResourceResponse(dto.targetType(), dto.targetId());
  }

}
