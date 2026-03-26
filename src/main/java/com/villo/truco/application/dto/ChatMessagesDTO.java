package com.villo.truco.application.dto;

import com.villo.truco.domain.model.chat.Chat;
import java.util.List;

public record ChatMessagesDTO(String chatId, String parentType, String parentId,
                              List<ChatMessageDTO> messages) {

    public static ChatMessagesDTO of(final Chat chat) {

        return new ChatMessagesDTO(chat.getId().value().toString(), chat.getParentType().name(),
            chat.getParentId(),
            chat.getMessages().stream().map(ChatMessageDTO::from).toList());
    }

}
