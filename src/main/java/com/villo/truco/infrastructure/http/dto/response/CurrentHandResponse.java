package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.CurrentHandDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cartas jugadas en la mano actual")
public record CurrentHandResponse(
    @Schema(description = "Carta jugada por player one") CardResponse cardPlayerOne,
    @Schema(description = "Carta jugada por player two") CardResponse cardPlayerTwo,
    @Schema(description = "Jugador que es mano en la mano actual", example = "player-1") String mano) {

  public static CurrentHandResponse from(final CurrentHandDTO dto) {

    return new CurrentHandResponse(
        dto.cardPlayerOne() != null ? CardResponse.from(dto.cardPlayerOne()) : null,
        dto.cardPlayerTwo() != null ? CardResponse.from(dto.cardPlayerTwo()) : null, dto.mano());
  }

}
