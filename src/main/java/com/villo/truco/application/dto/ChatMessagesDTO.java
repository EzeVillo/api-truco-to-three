package com.villo.truco.application.dto;

import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.domain.model.chat.ChatReadView;
import java.util.List;

public record ChatMessagesDTO(String chatId, String parentType, String parentId,
                              List<ChatMessageDTO> messages) {

  public static ChatMessagesDTO of(final ChatReadView chat,
      final PublicActorResolver publicActorResolver) {

    return new ChatMessagesDTO(chat.id().value().toString(), chat.parentType().name(),
        chat.parentId(),
        chat.messages().stream().map(message -> ChatMessageDTO.from(message, publicActorResolver))
            .toList());
  }

}
