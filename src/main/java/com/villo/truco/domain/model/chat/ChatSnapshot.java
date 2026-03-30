package com.villo.truco.domain.model.chat;

import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record ChatSnapshot(ChatId id, ChatParentType parentType, String parentId,
                           Set<PlayerId> participants, List<ChatMessageSnapshot> messages,
                           Map<PlayerId, Instant> lastMessageTimestamps,
                           Duration rateLimitCooldown) {

}
