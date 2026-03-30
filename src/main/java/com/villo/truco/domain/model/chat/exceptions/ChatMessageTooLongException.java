package com.villo.truco.domain.model.chat.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class ChatMessageTooLongException extends DomainException {

    public ChatMessageTooLongException(final int actualLength, final int maxLength) {

        super("Message too long: " + actualLength + " characters, max allowed: " + maxLength);
    }

}
