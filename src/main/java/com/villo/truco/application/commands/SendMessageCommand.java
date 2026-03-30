package com.villo.truco.application.commands;

import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record SendMessageCommand(ChatId chatId, PlayerId playerId, String content) {

    public SendMessageCommand {

        Objects.requireNonNull(chatId);
        Objects.requireNonNull(playerId);
        Objects.requireNonNull(content);
    }

    public SendMessageCommand(final String chatId, final String playerId, final String content) {

        this(ChatId.of(chatId), PlayerId.of(playerId), content);
    }

}
