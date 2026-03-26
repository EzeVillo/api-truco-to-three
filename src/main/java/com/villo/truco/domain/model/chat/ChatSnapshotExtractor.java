package com.villo.truco.domain.model.chat;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public final class ChatSnapshotExtractor {

    private ChatSnapshotExtractor() {

    }

    public static ChatSnapshot extract(final Chat chat) {

        final var messages = chat.getMessages().stream()
            .map(ChatSnapshotExtractor::extractMessage).toList();

        return new ChatSnapshot(chat.getId(), chat.getParentType(), chat.getParentId(),
            new LinkedHashSet<>(chat.getParticipants()), messages,
            new LinkedHashMap<>(chat.getLastMessageTimestamps()), chat.getRateLimitCooldown());
    }

    private static ChatMessageSnapshot extractMessage(final ChatMessage message) {

        return new ChatMessageSnapshot(message.getId(), message.getSenderId(), message.getContent(),
            message.getSentAt());
    }

}
