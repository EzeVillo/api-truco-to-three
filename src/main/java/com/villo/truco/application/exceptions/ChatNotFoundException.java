package com.villo.truco.application.exceptions;

import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;

public final class ChatNotFoundException extends ApplicationException {

    public ChatNotFoundException(final ChatId chatId) {

        super(ApplicationStatus.NOT_FOUND, "Chat not found: " + chatId);
    }

    public ChatNotFoundException(final ChatParentType parentType, final String parentId) {

        super(ApplicationStatus.NOT_FOUND,
            "Chat not found for " + parentType.name() + ": " + parentId);
    }

}
