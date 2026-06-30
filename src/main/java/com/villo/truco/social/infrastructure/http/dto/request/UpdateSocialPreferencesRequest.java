package com.villo.truco.social.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Preferencias sociales del jugador")
public record UpdateSocialPreferencesRequest(
    @NotNull @Schema(description = "Si el jugador acepta recibir solicitudes de amistad", example = "false") Boolean acceptsFriendRequests) {

}
