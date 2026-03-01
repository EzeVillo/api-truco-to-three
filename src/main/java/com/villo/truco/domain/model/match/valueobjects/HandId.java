package com.villo.truco.domain.model.match.valueobjects;

import java.util.UUID;

public record HandId(UUID value) {

    public static HandId generate() {

        return new HandId(UUID.randomUUID());
    }

}