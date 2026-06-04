package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.RoundStateDTO;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Estado de la ronda en curso")
public record RoundStateResponse(
    @Schema(description = "Estado interno de la ronda", example = "IN_PROGRESS") String status,
    @Schema(description = "Nombre visible del jugador al turno", example = "juancho") String currentTurn,
    @ArraySchema(schema = @Schema(implementation = CardResponse.class), arraySchema = @Schema(description = "Cartas en mano del jugador autenticado")) List<CardResponse> myCards,
    @Schema(description = "Estado de resolucion de la ronda", example = "PLAYING") String roundStatus,
    @Schema(description = "Ultimo canto de truco activo", example = "TRUCO") String currentTrucoCall,
    @Schema(description = "Canto de envido pendiente de respuesta; null si no hay envido en curso o ya se resolvio", example = "REAL_ENVIDO") String currentEnvidoCall,
    @Schema(description = "Nombre visible del ganador de la ronda, si existe", example = "juancho") String winner,
    @ArraySchema(schema = @Schema(implementation = AvailableActionResponse.class), arraySchema = @Schema(description = "Acciones permitidas para el jugador")) List<AvailableActionResponse> availableActions,
    @ArraySchema(schema = @Schema(implementation = PlayedHandResponse.class), arraySchema = @Schema(description = "Historial de manos jugadas")) List<PlayedHandResponse> playedHands,
    @Schema(description = "Estado parcial de la mano actual") CurrentHandResponse currentHand,
    @Schema(description = "Instante (epochMillis) en que el asiento que debe actuar pierde por timeout; null si no corre reloj", example = "1772768188123") Long actionDeadline,
    @Schema(description = "Duración total del plazo del turno en milisegundos; null si no corre reloj", example = "30000") Long turnDurationMillis,
    @Schema(description = "Asiento al que aplica el reloj (PLAYER_ONE | PLAYER_TWO); null si no corre reloj", example = "PLAYER_ONE") String actionDeadlineSeat) {

  public static RoundStateResponse from(final RoundStateDTO dto) {

    return new RoundStateResponse(dto.status(), dto.currentTurn(),
        dto.myCards().stream().map(CardResponse::from).toList(), dto.roundStatus(),
        dto.currentTrucoCall(), dto.currentEnvidoCall(), dto.winner(),
        dto.availableActions().stream().map(AvailableActionResponse::from).toList(),
        dto.playedHands().stream().map(PlayedHandResponse::from).toList(),
        dto.currentHand() != null ? CurrentHandResponse.from(dto.currentHand()) : null,
        dto.actionDeadline(), dto.turnDurationMillis(), dto.actionDeadlineSeat());
  }

}
