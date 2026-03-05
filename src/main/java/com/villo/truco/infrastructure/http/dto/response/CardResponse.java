package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.CardDTO;

public record CardResponse(String suit, int number) {

  public static CardResponse from(final CardDTO dto) {

    return new CardResponse(dto.suit(), dto.number());
  }

}
