package com.villo.truco.domain.model.chat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;

public final class ChatRehydrator {

  private ChatRehydrator() {

  }

  public static Chat rehydrate(final ChatSnapshot snapshot) {

    final var messages = new ArrayList<>(snapshot.messages().stream()
        .map(ms -> ChatMessage.reconstruct(ms.id(), ms.senderId(), ms.content(), ms.sentAt()))
        .toList());

    return Chat.reconstruct(snapshot.id(), snapshot.parentType(), snapshot.parentId(),
        new LinkedHashSet<>(snapshot.participants()), messages,
        new HashMap<>(snapshot.lastMessageTimestamps()), snapshot.rateLimitCooldown());
  }

}
