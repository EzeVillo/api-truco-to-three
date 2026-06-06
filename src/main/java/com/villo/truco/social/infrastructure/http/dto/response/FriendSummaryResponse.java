package com.villo.truco.social.infrastructure.http.dto.response;

import com.villo.truco.social.application.dto.FriendSummaryDTO;
import com.villo.truco.social.application.dto.SpectatableMatchRefDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos de un amigo")
public record FriendSummaryResponse(
    @Schema(description = "Nombre de usuario del amigo", example = "martina") String friendUsername,
    @Schema(description = "Partida activa espectable del amigo, si existe") SpectatableMatchRefResponse spectatableMatch) {

  public static FriendSummaryResponse from(final FriendSummaryDTO dto) {

    return new FriendSummaryResponse(dto.friendUsername(),
        SpectatableMatchRefResponse.from(dto.spectatableMatch()));
  }

  public record SpectatableMatchRefResponse(
      @Schema(description = "Identificador de la partida", example = "550e8400-e29b-41d4-a716-446655440000") String id,
      @Schema(description = "Estado de la partida", example = "IN_PROGRESS") String status) {

    private static SpectatableMatchRefResponse from(final SpectatableMatchRefDTO dto) {

      return dto == null ? null : new SpectatableMatchRefResponse(dto.id(), dto.status());
    }

  }

}
