package com.villo.truco.social.infrastructure.http.dto.response;

import com.villo.truco.social.application.dto.SocialPreferencesDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Preferencias sociales del jugador")
public record SocialPreferencesResponse(
    @Schema(description = "Si el jugador acepta recibir solicitudes de amistad", example = "true") boolean acceptsFriendRequests) {

  public static SocialPreferencesResponse from(final SocialPreferencesDTO dto) {

    return new SocialPreferencesResponse(dto.acceptsFriendRequests());
  }

}
