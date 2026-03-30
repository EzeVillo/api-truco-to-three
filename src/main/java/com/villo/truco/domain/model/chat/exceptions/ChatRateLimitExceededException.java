package com.villo.truco.domain.model.chat.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class ChatRateLimitExceededException extends DomainException {

    public ChatRateLimitExceededException() {

        super("Message rate limit exceeded, please wait before sending another message");
    }

}
