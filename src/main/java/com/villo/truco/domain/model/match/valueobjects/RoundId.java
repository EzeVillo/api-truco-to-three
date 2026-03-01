package com.villo.truco.domain.model.match.valueobjects;

import java.util.UUID;

public record RoundId(UUID value) {

    public static RoundId generate() {

        return new RoundId(UUID.randomUUID());
    }

}
