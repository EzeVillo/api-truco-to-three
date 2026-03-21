package com.villo.truco.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Solicitud de registro de usuario")
public record RegisterUserRequest(
    @Schema(description = "Nombre de usuario único", example = "juancho") String username,
    @Schema(description = "Contraseña del usuario", example = "miPassword123") String password) {

}
