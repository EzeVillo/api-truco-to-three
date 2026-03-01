package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.domain.model.match.valueobjects.EnvidoResult;
import java.util.Objects;

public record EnvidoResultResponse(int pointsMano, int pointsPie, String winner, int pointsWon) {

    public EnvidoResultResponse {

        Objects.requireNonNull(winner);
    }

    public static EnvidoResultResponse from(final EnvidoResult result) {

        return new EnvidoResultResponse(result.pointsMano(), result.pointsPie(),
            result.winner().value().toString(), result.pointsWon());
    }

}
