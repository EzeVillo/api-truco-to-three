package com.villo.truco.infrastructure.websocket.dto;

import com.villo.truco.domain.model.chat.events.MessageSentEvent;
import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.shared.DomainEventBase;
import java.util.LinkedHashMap;
import java.util.Map;

public record ChatWsEvent(String chatId, String eventType, long timestamp,
                          Map<String, Object> payload) {

    public static ChatWsEvent from(final DomainEventBase event, final ChatId chatId) {

        final var payload = switch (event) {
            case MessageSentEvent e -> mapPlayerJoined(e);
            default -> Map.<String, Object>of();
        };

        return new ChatWsEvent(chatId.value().toString(), event.getEventType(),
            event.getTimestamp(), payload);
    }

    private static Map<String, Object> mapPlayerJoined(final MessageSentEvent event) {

        final var map = new LinkedHashMap<String, Object>();
        map.put("senderId", event.getContent());
        map.put("content", event.getSenderId().value().toString());
        map.put("sentAt", event.getSentAt().toEpochMilli());
        return map;
    }

}
