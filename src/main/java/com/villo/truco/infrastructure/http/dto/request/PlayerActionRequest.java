package com.villo.truco.infrastructure.http.dto.request;

import java.util.Objects;

public record PlayerActionRequest(String playerId) {

    public PlayerActionRequest {

        Objects.requireNonNull(playerId);
    }

}
