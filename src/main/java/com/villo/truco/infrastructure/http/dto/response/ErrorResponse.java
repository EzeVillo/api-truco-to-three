package com.villo.truco.infrastructure.http.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Respuesta de error estándar de la API")
public record ErrorResponse(
    @Schema(description = "Código o tipo de error", example = "com.villo.truco.application.exceptions.UnauthorizedAccessException") String errorCode,
    @Schema(description = "Detalle del error", example = "Missing authentication token") String message,
    @Schema(description = "Fecha y hora UTC del error", example = "2026-03-06T03:15:30Z") Instant timestamp) {

}
