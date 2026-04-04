package com.villo.truco.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Solicitud para crear una copa de eliminacion directa")
public record CreateCupRequest(
    @NotNull @Min(2) @Schema(description = "Numero de jugadores de la copa", example = "4") Integer numberOfPlayers,
    @NotNull @Min(1) @Schema(description = "Partidas a jugar por match", example = "3") Integer gamesToPlay,
    @NotNull @Schema(description = "Visibilidad de la sala", example = "PRIVATE", allowableValues = {
        "PUBLIC", "PRIVATE"}) String visibility) {

}
