package com.villo.truco.infrastructure.http.dto.request;

import java.util.Objects;

public record PlayCardRequest(String playerId, String suit, int number) {

    public PlayCardRequest {

        Objects.requireNonNull(playerId);
        Objects.requireNonNull(suit);
    }

}
