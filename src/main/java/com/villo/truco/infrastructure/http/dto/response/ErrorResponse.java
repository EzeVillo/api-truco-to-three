package com.villo.truco.infrastructure.http.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Respuesta de error estándar de la API")
public record ErrorResponse(
    @Schema(description = "Código o tipo de error", example = "com.villo.truco.application.exceptions.UnauthorizedAccessException") String errorCode,
    @Schema(description = "Detalle del error", example = "Missing authentication token") String message,
    @Schema(description = "Fecha y hora UTC del error", example = "2026-03-06T03:15:30Z") Instant timestamp,
    @Schema(description = "Identificador único de request", example = "b1f4d7a0-2f29-4e8f-b8ea-a302f9084f3b") String requestId) {

}
