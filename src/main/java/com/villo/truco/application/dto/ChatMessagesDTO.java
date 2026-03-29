package com.villo.truco.application.dto;

import com.villo.truco.domain.model.chat.ChatReadView;
import java.util.List;

public record ChatMessagesDTO(String chatId, String parentType, String parentId,
                              List<ChatMessageDTO> messages) {

  public static ChatMessagesDTO of(final ChatReadView chat) {

    return new ChatMessagesDTO(chat.id().value().toString(), chat.parentType().name(),
        chat.parentId(), chat.messages().stream().map(ChatMessageDTO::from).toList());
  }

}
