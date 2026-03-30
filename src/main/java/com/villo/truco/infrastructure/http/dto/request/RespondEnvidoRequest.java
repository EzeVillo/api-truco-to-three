package com.villo.truco.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Solicitud para responder un canto de envido")
public record RespondEnvidoRequest(
    @NotBlank @Schema(description = "Respuesta al envido", example = "QUIERO", allowableValues = {
        "QUIERO", "NO_QUIERO"}) String response) {

}
