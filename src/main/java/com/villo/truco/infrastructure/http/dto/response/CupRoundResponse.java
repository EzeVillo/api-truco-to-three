package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.CupRoundDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Ronda del bracket de copa")
public record CupRoundResponse(
    @Schema(description = "Número de ronda", example = "1") int roundNumber,
    @Schema(description = "Nombre legible de la ronda", example = "Semifinal") String roundName,
    @Schema(description = "Cruces de la ronda") List<CupBoutResponse> bouts) {

  public static CupRoundResponse from(final CupRoundDTO dto) {

    return new CupRoundResponse(dto.roundNumber(), dto.roundName(),
        dto.bouts().stream().map(CupBoutResponse::from).toList());
  }

}
