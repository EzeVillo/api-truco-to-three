package com.villo.truco.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Solicitud para jugar una carta")
public record PlayCardRequest(
    @Schema(description = "Palo de la carta", example = "ESPADA") String suit,
    @Schema(description = "Número de la carta", example = "1") int number) {

  public PlayCardRequest {

    Objects.requireNonNull(suit);
  }

}
