package com.villo.truco.infrastructure.websocket;

import com.villo.truco.application.ports.out.ChatDomainEventHandler;
import com.villo.truco.application.ports.out.ChatEventContext;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import com.villo.truco.infrastructure.websocket.dto.ChatWsEvent;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public final class StompChatEventNotifier implements ChatDomainEventHandler<DomainEventBase> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StompChatEventNotifier.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final EventNotifierHealthRegistry healthRegistry;

    public StompChatEventNotifier(final SimpMessagingTemplate messagingTemplate,
        final EventNotifierHealthRegistry healthRegistry) {

        this.messagingTemplate = Objects.requireNonNull(messagingTemplate);
        this.healthRegistry = Objects.requireNonNull(healthRegistry);
    }

    @Override
    public Class<DomainEventBase> eventType() {

        return DomainEventBase.class;
    }

    @Override
    public void handle(final DomainEventBase event, final ChatEventContext context) {

        LOGGER.debug("Publishing chat event for chatId={} type={}", context.chatId(),
            event.getEventType());
        final var wsEvent = ChatWsEvent.from(event, context.chatId());
        for (final var participant : context.participants()) {
            sendEvent(participant, wsEvent);
        }
    }

    private void sendEvent(final PlayerId playerId, final Object message) {

        final var userName = WebSocketUserNaming.userName(playerId);
        LOGGER.debug("Sending chat WS event to user={} type={}", userName,
            message.getClass().getSimpleName());
        try {
            this.messagingTemplate.convertAndSendToUser(userName, "/queue/chat", message);
            this.healthRegistry.recordSuccess();
        } catch (final RuntimeException ex) {
            this.healthRegistry.recordFailure(ex);
            throw ex;
        }
    }

}
