package com.villo.truco.domain.model.chat.valueobjects;

import com.villo.truco.domain.shared.exceptions.InvalidIdException;
import java.util.UUID;

public record ChatId(UUID value) {

    public static ChatId of(final String value) {

        try {
            return new ChatId(UUID.fromString(value));
        } catch (final IllegalArgumentException e) {
            throw new InvalidIdException(value);
        }
    }

    public static ChatId generate() {

        return new ChatId(UUID.randomUUID());
    }

}
