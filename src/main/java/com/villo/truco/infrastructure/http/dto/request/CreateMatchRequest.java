package com.villo.truco.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Datos para crear una partida")
public record CreateMatchRequest(
    @NotNull @Min(1) @Schema(description = "Cantidad total de games de la serie (opciones: 1, 3, 5)", example = "3", allowableValues = {
        "1", "3", "5"}) Integer gamesToPlay,
    @NotNull @Schema(description = "Visibilidad de la sala", example = "PRIVATE", allowableValues = {
        "PUBLIC", "PRIVATE"}) String visibility) {

}
