package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.RematchSessionStateDTO;
import com.villo.truco.domain.model.rematch.valueobjects.RematchPlayerChoice;
import com.villo.truco.domain.model.rematch.valueobjects.RematchSessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Estado de la session de revancha")
public record RematchSessionResponse(
    @Schema(description = "ID de la session de revancha", example = "550e8400-e29b-41d4-a716-446655440000") String sessionId,
    @Schema(description = "ID de la partida original", example = "550e8400-e29b-41d4-a716-446655440001") String originMatchId,
    @Schema(description = "Estado de la session", example = "OPEN") RematchSessionStatus status,
    @Schema(description = "Eleccion del jugador uno", example = "UNDECIDED") RematchPlayerChoice playerOneChoice,
    @Schema(description = "Eleccion del jugador dos", example = "WANTS_REMATCH") RematchPlayerChoice playerTwoChoice,
    @Schema(description = "Timestamp de expiracion (ISO-8601)", example = "2026-05-16T18:00:00Z") Instant expiresAt,
    @Schema(description = "ID de la nueva partida si fue confirmada", example = "550e8400-e29b-41d4-a716-446655440002", nullable = true) String resultMatchId) {

  public static RematchSessionResponse from(final RematchSessionStateDTO snapshot) {

    return new RematchSessionResponse(snapshot.sessionId(), snapshot.originMatchId(),
        snapshot.status(), snapshot.playerOneChoice(), snapshot.playerTwoChoice(),
        snapshot.expiresAt(), snapshot.resultMatchId());
  }

}
