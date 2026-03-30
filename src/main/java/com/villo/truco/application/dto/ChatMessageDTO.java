package com.villo.truco.application.dto;

import com.villo.truco.domain.model.chat.ChatMessageView;

public record ChatMessageDTO(String messageId, String senderId, String content, long sentAt) {

  public static ChatMessageDTO from(final ChatMessageView message) {

    return new ChatMessageDTO(message.id().value().toString(),
        message.senderId().value().toString(), message.content(), message.sentAt().toEpochMilli());
  }

}
