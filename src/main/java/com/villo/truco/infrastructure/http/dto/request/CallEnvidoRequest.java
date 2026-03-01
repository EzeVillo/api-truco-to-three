package com.villo.truco.infrastructure.http.dto.request;

import java.util.Objects;

public record CallEnvidoRequest(String playerId, String call) {

    public CallEnvidoRequest {

        Objects.requireNonNull(playerId, "PlayerId is required");
        Objects.requireNonNull(call, "Call is required");
    }

}
