package com.villo.truco.infrastructure.persistence.mappers;

import com.villo.truco.domain.model.chat.Chat;
import com.villo.truco.domain.model.chat.ChatMessageSnapshot;
import com.villo.truco.domain.model.chat.ChatRehydrator;
import com.villo.truco.domain.model.chat.ChatSnapshot;
import com.villo.truco.domain.model.chat.ChatSnapshotExtractor;
import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.model.chat.valueobjects.ChatMessageId;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.entities.ChatJpaEntity;
import com.villo.truco.infrastructure.persistence.entities.ChatMessageData;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ChatMapper {

    public ChatJpaEntity toEntity(final Chat chat) {

        final var snapshot = ChatSnapshotExtractor.extract(chat);
        final var entity = new ChatJpaEntity();

        entity.setId(snapshot.id().value());
        entity.setParentType(snapshot.parentType().name());
        entity.setParentId(snapshot.parentId());
        entity.setParticipants(
            snapshot.participants().stream().map(PlayerId::value).toList());
        entity.setMessages(
            snapshot.messages().stream().map(this::toMessageData).toList());

        final var timestamps = new HashMap<String, Long>();
        snapshot.lastMessageTimestamps()
            .forEach((playerId, instant) -> timestamps.put(playerId.value().toString(),
                instant.toEpochMilli()));
        entity.setLastMessageTimestamps(timestamps);

        entity.setRateLimitMs(snapshot.rateLimitCooldown().toMillis());
        entity.setVersion((int) chat.getVersion());

        return entity;
    }

    public Chat toDomain(final ChatJpaEntity entity) {

        final var participants = entity.getParticipants().stream()
            .map(PlayerId::new)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        final var messages = entity.getMessages().stream()
            .map(this::toMessageSnapshot)
            .toList();

        final var timestamps = new HashMap<PlayerId, Instant>();
        entity.getLastMessageTimestamps()
            .forEach((uuidStr, millis) -> timestamps.put(new PlayerId(UUID.fromString(uuidStr)),
                Instant.ofEpochMilli(millis)));

        final var snapshot = new ChatSnapshot(new ChatId(entity.getId()),
            ChatParentType.valueOf(entity.getParentType()), entity.getParentId(), participants,
            messages, timestamps, Duration.ofMillis(entity.getRateLimitMs()));

        final var chat = ChatRehydrator.rehydrate(snapshot);
        chat.setVersion(entity.getVersion());
        return chat;
    }

    private ChatMessageData toMessageData(final ChatMessageSnapshot message) {

        return new ChatMessageData(message.id().value(), message.senderId().value(),
            message.content(), message.sentAt().toEpochMilli());
    }

    private ChatMessageSnapshot toMessageSnapshot(final ChatMessageData data) {

        return new ChatMessageSnapshot(new ChatMessageId(data.id()), new PlayerId(data.senderId()),
            data.content(), Instant.ofEpochMilli(data.sentAt()));
    }

}
