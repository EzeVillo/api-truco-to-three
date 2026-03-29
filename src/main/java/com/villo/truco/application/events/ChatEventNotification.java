package com.villo.truco.application.events;

import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Map;

public record ChatEventNotification(ChatId chatId, List<PlayerId> recipients, String eventType,
                                    long timestamp, Map<String, Object> payload) implements
    ApplicationEvent {

}
