package com.villo.truco.domain.model.chat;

import com.villo.truco.domain.model.chat.valueobjects.ChatMessageId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;

public record ChatMessageSnapshot(ChatMessageId id, PlayerId senderId, String content,
                                  Instant sentAt) {

}
