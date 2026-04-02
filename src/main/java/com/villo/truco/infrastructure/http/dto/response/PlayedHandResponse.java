package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.PlayedHandDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado de una mano ya jugada")
public record PlayedHandResponse(
    @Schema(description = "Carta jugada por player one") CardResponse cardPlayerOne,
    @Schema(description = "Carta jugada por player two") CardResponse cardPlayerTwo,
    @Schema(description = "Nombre visible del ganador de la mano", example = "juancho") String winner) {

  public static PlayedHandResponse from(final PlayedHandDTO dto) {

    return new PlayedHandResponse(
        dto.cardPlayerOne() != null ? CardResponse.from(dto.cardPlayerOne()) : null,
        dto.cardPlayerTwo() != null ? CardResponse.from(dto.cardPlayerTwo()) : null, dto.winner());
  }

}
