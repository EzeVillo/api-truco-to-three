package com.villo.truco.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Solicitud para jugar una carta")
public record PlayCardRequest(
    @NotBlank @Schema(description = "Palo de la carta", example = "ESPADA", allowableValues = {
        "ESPADA", "BASTO", "COPA", "ORO"}) String suit,
    @NotNull @Min(1) @Schema(description = "Número de la carta", example = "1") Integer number) {

}
