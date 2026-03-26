package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.CupDomainEventHandler;
import com.villo.truco.application.ports.out.CupEventContext;
import com.villo.truco.domain.model.chat.Chat;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.model.cup.events.CupStartedEvent;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.ports.ChatRepository;
import java.util.LinkedHashSet;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChatCupStartedEventHandler implements CupDomainEventHandler<CupStartedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatCupStartedEventHandler.class);

    private final ChatRepository chatRepository;
    private final ChatQueryRepository chatQueryRepository;

    public ChatCupStartedEventHandler(final ChatRepository chatRepository,
        final ChatQueryRepository chatQueryRepository) {

        this.chatRepository = Objects.requireNonNull(chatRepository);
        this.chatQueryRepository = Objects.requireNonNull(chatQueryRepository);
    }

    @Override
    public Class<CupStartedEvent> eventType() {

        return CupStartedEvent.class;
    }

    @Override
    public void handle(final CupStartedEvent event, final CupEventContext context) {

        final var parentId = context.cupId().value().toString();
        final var existing = this.chatQueryRepository.findByParentTypeAndParentId(
            ChatParentType.CUP, parentId);

        if (existing.isPresent()) {
            LOGGER.debug("Chat already exists for cup={}", parentId);
            return;
        }

        final var participants = new LinkedHashSet<>(context.participants());
        final var chat = Chat.create(ChatParentType.CUP, parentId, participants);
        this.chatRepository.save(chat);
        LOGGER.info("Created chat for cup={}", parentId);
    }

}
