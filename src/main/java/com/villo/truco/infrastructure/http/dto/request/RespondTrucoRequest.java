package com.villo.truco.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Solicitud para responder un canto de truco")
public record RespondTrucoRequest(
    @NotBlank @Schema(description = "Respuesta al truco", example = "QUIERO") String response) {

}
