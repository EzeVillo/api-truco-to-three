package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.CupDomainEventHandler;
import com.villo.truco.application.ports.out.CupEventContext;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.model.cup.events.CupFinishedEvent;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.ports.ChatRepository;
import java.util.Objects;

public final class ChatCupFinishedEventHandler implements CupDomainEventHandler<CupFinishedEvent> {

    private final ChatRepository chatRepository;
    private final ChatQueryRepository chatQueryRepository;

    public ChatCupFinishedEventHandler(final ChatRepository chatRepository,
        final ChatQueryRepository chatQueryRepository) {

        this.chatRepository = Objects.requireNonNull(chatRepository);
        this.chatQueryRepository = Objects.requireNonNull(chatQueryRepository);
    }

    @Override
    public Class<CupFinishedEvent> eventType() {

        return CupFinishedEvent.class;
    }

    @Override
    public void handle(final CupFinishedEvent event, final CupEventContext context) {

        ChatLifecycleSupport.deleteChat(this.chatQueryRepository, this.chatRepository,
            ChatParentType.CUP, context.cupId().value().toString());
    }

}
