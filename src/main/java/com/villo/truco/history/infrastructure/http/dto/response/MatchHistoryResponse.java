package com.villo.truco.history.infrastructure.http.dto.response;

import com.villo.truco.history.application.dto.PlayerMatchHistoryDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Historial de las últimas partidas del jugador (máximo 5, más reciente primero)")
public record MatchHistoryResponse(
    @Schema(description = "Partidas recientes") List<MatchHistoryEntryResponse> entries) {

  public static MatchHistoryResponse from(final PlayerMatchHistoryDTO dto) {

    final var entries = dto.entries().stream().map(MatchHistoryEntryResponse::from).toList();
    return new MatchHistoryResponse(entries);
  }

}
