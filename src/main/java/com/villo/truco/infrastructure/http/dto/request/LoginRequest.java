package com.villo.truco.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Solicitud de login")
public record LoginRequest(
    @Schema(description = "Nombre de usuario", example = "juancho") String username,
    @Schema(description = "Contraseña del usuario") String password) {

}
