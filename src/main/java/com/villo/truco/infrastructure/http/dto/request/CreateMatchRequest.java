package com.villo.truco.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos para crear una partida")
public record CreateMatchRequest(
    @Schema(description = "Cantidad total de games de la serie (opciones: 1, 3, 5)", example = "3", allowableValues = {
        "1", "3", "5"}) int gamesToPlay) {

}
