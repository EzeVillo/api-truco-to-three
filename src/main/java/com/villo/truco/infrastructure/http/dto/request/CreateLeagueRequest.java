package com.villo.truco.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Solicitud para crear una liga")
public record CreateLeagueRequest(
    @NotNull @Min(2) @Schema(description = "Cantidad de jugadores de la liga", example = "4") Integer numberOfPlayers,
    @NotNull @Min(1) @Schema(description = "Cantidad de partidas por fixture (1, 3 o 5)", example = "3") Integer gamesToPlay,
    @NotNull @Schema(description = "Visibilidad de la sala", example = "PRIVATE", allowableValues = {
        "PUBLIC", "PRIVATE"}) String visibility) {

}
