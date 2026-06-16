package com.villo.truco.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Datos para crear una partida entre dos bots")
public record CreateBotVsBotMatchRequest(
    @NotBlank @Schema(description = "ID del primer bot", example = "00000000-0000-0000-0000-000000000001") String botOneId,
    @NotBlank @Schema(description = "ID del segundo bot (distinto del primero)", example = "00000000-0000-0000-0000-000000000002") String botTwoId,
    @NotNull @Min(1) @Schema(description = "Cantidad total de games de la serie (opciones: 1, 3, 5)", example = "3", allowableValues = {
        "1", "3", "5"}) Integer gamesToPlay) {

}
