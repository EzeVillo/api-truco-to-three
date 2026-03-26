package com.villo.truco.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "chats")
public class ChatJpaEntity {

    @Id
    private UUID id;

    @Column(name = "parent_type", nullable = false)
    private String parentType;

    @Column(name = "parent_id", nullable = false)
    private String parentId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "participants", columnDefinition = "jsonb", nullable = false)
    private List<UUID> participants;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "messages", columnDefinition = "jsonb", nullable = false)
    private List<ChatMessageData> messages;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "last_message_timestamps", columnDefinition = "jsonb", nullable = false)
    private Map<String, Long> lastMessageTimestamps;

    @Column(name = "rate_limit_ms", nullable = false)
    private long rateLimitMs;

    @Version
    private int version;

    public ChatJpaEntity() {

    }

    public UUID getId() {

        return this.id;
    }

    public void setId(final UUID id) {

        this.id = id;
    }

    public String getParentType() {

        return this.parentType;
    }

    public void setParentType(final String parentType) {

        this.parentType = parentType;
    }

    public String getParentId() {

        return this.parentId;
    }

    public void setParentId(final String parentId) {

        this.parentId = parentId;
    }

    public List<UUID> getParticipants() {

        return this.participants;
    }

    public void setParticipants(final List<UUID> participants) {

        this.participants = participants;
    }

    public List<ChatMessageData> getMessages() {

        return this.messages;
    }

    public void setMessages(final List<ChatMessageData> messages) {

        this.messages = messages;
    }

    public Map<String, Long> getLastMessageTimestamps() {

        return this.lastMessageTimestamps;
    }

    public void setLastMessageTimestamps(final Map<String, Long> lastMessageTimestamps) {

        this.lastMessageTimestamps = lastMessageTimestamps;
    }

    public long getRateLimitMs() {

        return this.rateLimitMs;
    }

    public void setRateLimitMs(final long rateLimitMs) {

        this.rateLimitMs = rateLimitMs;
    }

    public int getVersion() {

        return this.version;
    }

    public void setVersion(final int version) {

        this.version = version;
    }

}
