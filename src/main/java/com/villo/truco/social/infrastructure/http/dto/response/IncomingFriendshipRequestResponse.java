package com.villo.truco.social.infrastructure.http.dto.response;

import com.villo.truco.social.application.dto.IncomingFriendshipRequestDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Solicitud de amistad recibida")
public record IncomingFriendshipRequestResponse(
    @Schema(description = "Nombre de usuario del solicitante", example = "juancho") String requesterUsername) {

  public static IncomingFriendshipRequestResponse from(final IncomingFriendshipRequestDTO dto) {

    return new IncomingFriendshipRequestResponse(dto.requesterUsername());
  }

}
