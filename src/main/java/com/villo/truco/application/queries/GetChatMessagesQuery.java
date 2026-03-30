package com.villo.truco.application.queries;

import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record GetChatMessagesQuery(ChatId chatId, PlayerId requestingPlayer) {

    public GetChatMessagesQuery {

        Objects.requireNonNull(chatId);
        Objects.requireNonNull(requestingPlayer);
    }

    public GetChatMessagesQuery(final String chatId, final String requestingPlayer) {

        this(ChatId.of(chatId), PlayerId.of(requestingPlayer));
    }

}
