package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.LeagueFixtureDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fixture de liga")
public record LeagueFixtureResponse(
    @Schema(description = "ID del fixture", example = "fixture-123") String fixtureId,
    @Schema(description = "Número de jornada", example = "1") int matchdayNumber,
    @Schema(description = "ID del jugador local", example = "player-1") String playerOneId,
    @Schema(description = "ID del jugador visitante", example = "player-2") String playerTwoId,
    @Schema(description = "ID de partida asociada", example = "match-123") String matchId,
    @Schema(description = "ID del ganador si ya terminó", example = "player-1") String winnerPlayerId,
    @Schema(description = "Estado del fixture", example = "SCHEDULED") String status) {

  public static LeagueFixtureResponse from(final LeagueFixtureDTO dto) {

    return new LeagueFixtureResponse(dto.fixtureId(), dto.matchdayNumber(), dto.playerOneId(),
        dto.playerTwoId(), dto.matchId(), dto.winnerPlayerId(), dto.status());
  }

}
