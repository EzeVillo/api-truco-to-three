package com.villo.truco.domain.model.chat.valueobjects;

import com.villo.truco.domain.shared.exceptions.InvalidIdException;
import java.util.UUID;

public record ChatMessageId(UUID value) {

    public static ChatMessageId of(final String value) {

        try {
            return new ChatMessageId(UUID.fromString(value));
        } catch (final IllegalArgumentException e) {
            throw new InvalidIdException(value);
        }
    }

    public static ChatMessageId generate() {

        return new ChatMessageId(UUID.randomUUID());
    }

}
