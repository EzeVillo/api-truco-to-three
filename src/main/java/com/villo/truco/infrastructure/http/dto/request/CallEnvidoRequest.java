package com.villo.truco.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Solicitud para cantar envido")
public record CallEnvidoRequest(
    @NotBlank @Schema(description = "Tipo de canto envido", example = "ENVIDO", allowableValues = {
        "ENVIDO", "REAL_ENVIDO", "FALTA_ENVIDO"}) String call) {

}
