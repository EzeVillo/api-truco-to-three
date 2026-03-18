package com.villo.truco.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Solicitud para crear un torneo")
public record CreateTournamentRequest(
    @Schema(description = "Cantidad de jugadores del torneo", example = "4") int numberOfPlayers,
    @Schema(description = "Cantidad de partidas por fixture (1, 3 o 5)", example = "3") int gamesToPlay) {

}
