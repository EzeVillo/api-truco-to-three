package com.villo.truco.application.dto;

import com.villo.truco.domain.model.chat.ChatMessage;

public record ChatMessageDTO(String messageId, String senderId, String content, long sentAt) {

    public static ChatMessageDTO from(final ChatMessage message) {

        return new ChatMessageDTO(message.getId().value().toString(),
            message.getSenderId().value().toString(), message.getContent(),
            message.getSentAt().toEpochMilli());
    }

}
