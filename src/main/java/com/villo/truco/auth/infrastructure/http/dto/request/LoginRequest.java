package com.villo.truco.auth.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Solicitud de login")
public record LoginRequest(
    @NotBlank @Pattern(regexp = "^[A-Za-z0-9]+$", message = "must contain only letters and numbers") @Pattern(regexp = "^(?=(?:.*[A-Za-z]){3,}).+$", message = "must contain at least 3 letters") @Schema(description = "Nombre de usuario", example = "juancho") String username,
    @NotBlank @Schema(description = "Contraseña del usuario", example = "miPassword123") String password) {

}
