package com.villo.truco.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Solicitud de acceso como invitado")
public record GuestLoginRequest(
    @Schema(description = "Nombre opcional para mostrar a otros jugadores", example = "Invitado") String displayName) {

}
