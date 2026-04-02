package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.CupStateDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Estado completo de la copa")
public record CupStateResponse(
    @Schema(description = "ID de la copa", example = "cup-abc123") String cupId,
    @Schema(description = "Estado de la copa", example = "IN_PROGRESS") String status,
    @Schema(description = "Rondas del bracket") List<CupRoundResponse> rounds,
    @Schema(description = "Nombre visible del campeon (cuando la copa finaliza)", example = "juancho") String champion) {

  public static CupStateResponse from(final CupStateDTO dto) {

    return new CupStateResponse(dto.cupId(), dto.status(),
        dto.rounds().stream().map(CupRoundResponse::from).toList(), dto.champion());
  }

}
