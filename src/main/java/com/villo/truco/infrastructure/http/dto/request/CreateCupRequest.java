package com.villo.truco.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Solicitud para crear una copa de eliminación directa")
public record CreateCupRequest(
    @Schema(description = "Número de jugadores de la copa", example = "4") int numberOfPlayers,
    @Schema(description = "Partidas a jugar por match", example = "3") int gamesToPlay) {

}
