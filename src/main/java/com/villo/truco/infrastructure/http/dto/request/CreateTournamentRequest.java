package com.villo.truco.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Solicitud para crear un torneo")
public record CreateTournamentRequest(
    @Schema(description = "Capacidad del torneo (cantidad de jugadores)", example = "4") int capacity,
    @Schema(description = "Cantidad de partidas por fixture (1, 3 o 5)", example = "3") int gamesToPlay) {

}
