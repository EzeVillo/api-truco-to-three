package com.villo.truco.application.dto;

import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.domain.model.chat.ChatMessageView;

public record ChatMessageDTO(String messageId, String sender, String content, long sentAt) {

  public static ChatMessageDTO from(final ChatMessageView message,
      final PublicActorResolver publicActorResolver) {

    return new ChatMessageDTO(message.id().value().toString(),
        publicActorResolver.resolve(message.senderId()), message.content(),
        message.sentAt().toEpochMilli());
  }

}
