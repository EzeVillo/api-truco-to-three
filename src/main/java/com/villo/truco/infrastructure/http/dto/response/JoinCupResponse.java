package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.JoinCupDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta al unirse a una copa")
public record JoinCupResponse(
    @Schema(description = "ID de la copa", example = "cup-abc123") String cupId) {

  public static JoinCupResponse from(final JoinCupDTO dto) {

    return new JoinCupResponse(dto.cupId());
  }

}
