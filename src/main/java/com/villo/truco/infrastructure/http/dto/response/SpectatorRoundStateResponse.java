package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.SpectatorRoundStateDTO;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Estado de la ronda en curso para espectador")
public record SpectatorRoundStateResponse(
    @Schema(description = "Estado interno de la ronda", example = "IN_PROGRESS") String status,
    @Schema(description = "ID del jugador al turno", example = "juancho") String currentTurn,
    @Schema(description = "Estado de resolución de la ronda", example = "PLAYING") String roundStatus,
    @Schema(description = "Último canto de truco activo", example = "TRUCO") String currentTrucoCall,
    @Schema(description = "Ganador de la ronda, si existe", example = "juancho") String winner,
    @ArraySchema(schema = @Schema(implementation = PlayedHandResponse.class), arraySchema = @Schema(description = "Historial de manos jugadas")) List<PlayedHandResponse> playedHands,
    @Schema(description = "Estado parcial de la mano actual") CurrentHandResponse currentHand,
    @Schema(description = "Instante (epochMillis) en que el asiento que debe actuar pierde por timeout; null si no corre reloj", example = "1772768188123") Long actionDeadline,
    @Schema(description = "Duración total del plazo del turno en milisegundos; null si no corre reloj", example = "30000") Long turnDurationMillis,
    @Schema(description = "Asiento al que aplica el reloj (PLAYER_ONE | PLAYER_TWO); null si no corre reloj", example = "PLAYER_ONE") String actionDeadlineSeat) {

  public static SpectatorRoundStateResponse from(final SpectatorRoundStateDTO dto) {

    return new SpectatorRoundStateResponse(dto.status(), dto.currentTurn(), dto.roundStatus(),
        dto.currentTrucoCall(), dto.winner(),
        dto.playedHands().stream().map(PlayedHandResponse::from).toList(),
        dto.currentHand() != null ? CurrentHandResponse.from(dto.currentHand()) : null,
        dto.actionDeadline(), dto.turnDurationMillis(), dto.actionDeadlineSeat());
  }

}
