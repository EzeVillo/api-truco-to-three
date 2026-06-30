package com.villo.truco.history.infrastructure.http.dto.response;

import com.villo.truco.history.application.dto.MatchHistoryEntryDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Resumen de una partida del historial, visto desde el jugador consultante")
public record MatchHistoryEntryResponse(
    @Schema(description = "Identificador de la partida", example = "8b9c5936-9a1f-45ec-a587-24306689f6f7") UUID matchId,
    @Schema(description = "Nombre del rival (username o nombre del bot)", example = "juancho") String opponentName,
    @Schema(description = "Indica si el rival fue un bot", example = "false") boolean opponentIsBot,
    @Schema(description = "Resultado para el jugador", allowableValues = {"WON",
        "LOST"}, example = "WON") String outcome,
    @Schema(description = "Cómo terminó la partida", allowableValues = {"FINISHED", "ABANDONED",
        "FORFEITED"}, example = "FINISHED") String endReason,
    @Schema(description = "Juegos ganados por el jugador", example = "3") int ownGamesWon,
    @Schema(description = "Juegos ganados por el rival", example = "1") int opponentGamesWon,
    @Schema(description = "Fecha de fin (epoch millis)", example = "1772768158123") long endedAt) {

  public static MatchHistoryEntryResponse from(final MatchHistoryEntryDTO dto) {

    return new MatchHistoryEntryResponse(dto.matchId(), dto.opponentName(), dto.opponentIsBot(),
        dto.outcome(), dto.endReason(), dto.ownGamesWon(), dto.opponentGamesWon(), dto.endedAt());
  }

}
