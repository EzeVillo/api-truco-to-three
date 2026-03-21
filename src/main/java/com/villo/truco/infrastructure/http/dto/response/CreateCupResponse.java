package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.CreateCupDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta al crear copa")
public record CreateCupResponse(
    @Schema(description = "ID de la copa", example = "cup-abc123") String cupId,
    @Schema(description = "Código de invitación de la copa", example = "ABCD1234") String inviteCode) {

  public static CreateCupResponse from(final CreateCupDTO dto) {

    return new CreateCupResponse(dto.cupId(), dto.inviteCode());
  }

}
