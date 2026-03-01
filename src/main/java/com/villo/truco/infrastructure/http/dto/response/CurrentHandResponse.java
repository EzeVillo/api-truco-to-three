package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.CurrentHandDTO;

public record CurrentHandResponse(CardResponse cardPlayerOne, CardResponse cardPlayerTwo,
                                  String mano) {

    public static CurrentHandResponse from(final CurrentHandDTO dto) {

        return new CurrentHandResponse(
            dto.cardPlayerOne() != null ? CardResponse.from(dto.cardPlayerOne()) : null,
            dto.cardPlayerTwo() != null ? CardResponse.from(dto.cardPlayerTwo()) : null,
            dto.mano());
    }

}
