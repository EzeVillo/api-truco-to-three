package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.CardDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Carta")
public record CardResponse(@Schema(description = "Palo", example = "ESPADA") String suit,
                           @Schema(description = "Número", example = "1") int number) {

  public static CardResponse from(final CardDTO dto) {

    return new CardResponse(dto.suit(), dto.number());
  }

}
