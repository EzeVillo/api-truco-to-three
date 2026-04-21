package com.villo.truco.social.infrastructure.http.dto.response;

import com.villo.truco.social.application.dto.FriendSummaryDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos de un amigo")
public record FriendSummaryResponse(
    @Schema(description = "Nombre de usuario del amigo", example = "martina") String friendUsername) {

  public static FriendSummaryResponse from(final FriendSummaryDTO dto) {

    return new FriendSummaryResponse(dto.friendUsername());
  }

}
