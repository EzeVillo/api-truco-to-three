package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.PlayedHandDTO;

public record PlayedHandResponse(CardResponse cardPlayerOne, CardResponse cardPlayerTwo,
                                 String winner) {

  public static PlayedHandResponse from(final PlayedHandDTO dto) {

    return new PlayedHandResponse(
        dto.cardPlayerOne() != null ? CardResponse.from(dto.cardPlayerOne()) : null,
        dto.cardPlayerTwo() != null ? CardResponse.from(dto.cardPlayerTwo()) : null, dto.winner());
  }

}
