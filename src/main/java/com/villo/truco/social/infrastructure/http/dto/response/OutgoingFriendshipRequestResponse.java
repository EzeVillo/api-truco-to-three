package com.villo.truco.social.infrastructure.http.dto.response;

import com.villo.truco.social.application.dto.OutgoingFriendshipRequestDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Solicitud de amistad enviada")
public record OutgoingFriendshipRequestResponse(
    @Schema(description = "Nombre de usuario del destinatario", example = "martina") String addresseeUsername) {

  public static OutgoingFriendshipRequestResponse from(final OutgoingFriendshipRequestDTO dto) {

    return new OutgoingFriendshipRequestResponse(dto.addresseeUsername());
  }

}
