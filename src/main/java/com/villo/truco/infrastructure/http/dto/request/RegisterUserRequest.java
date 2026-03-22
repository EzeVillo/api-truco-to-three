package com.villo.truco.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Solicitud de registro de usuario")
public record RegisterUserRequest(
    @NotBlank @Schema(description = "Nombre de usuario único", example = "juancho") String username,
    @NotBlank @Schema(description = "Contraseña del usuario", example = "miPassword123") String password) {

}
