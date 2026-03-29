package com.villo.truco.application.eventhandlers;

import com.villo.truco.domain.model.chat.events.ChatCreatedEvent;
import com.villo.truco.domain.model.chat.events.MessageSentEvent;
import com.villo.truco.domain.shared.DomainEventBase;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ChatEventMapper {

  private static Map<String, Object> mapChatCreated(final ChatCreatedEvent event) {

    final var payload = new LinkedHashMap<String, Object>();
    payload.put("parentType", event.getParentType().name());
    payload.put("parentId", event.getParentId());
    return payload;
  }

  private static Map<String, Object> mapMessageSent(final MessageSentEvent event) {

    final var payload = new LinkedHashMap<String, Object>();
    payload.put("senderId", event.getSenderId().value().toString());
    payload.put("content", event.getContent());
    payload.put("sentAt", event.getSentAt().toEpochMilli());
    return payload;
  }

  public Map<String, Object> map(final DomainEventBase event) {

    return switch (event) {
      case ChatCreatedEvent e -> mapChatCreated(e);
      case MessageSentEvent e -> mapMessageSent(e);
      default -> Map.of();
    };
  }

}
