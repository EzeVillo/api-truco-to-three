package com.villo.truco.social.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Solicitud de amistad")
public record RequestFriendshipRequest(
    @NotBlank @Schema(description = "Nombre de usuario del destinatario", example = "juancho") String username) {

}
