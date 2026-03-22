package com.villo.truco.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Solicitud de login")
public record LoginRequest(
    @NotBlank @Schema(description = "Nombre de usuario", example = "juancho") String username,
    @NotBlank @Schema(description = "Contraseña del usuario", example = "miPassword123") String password) {

}
