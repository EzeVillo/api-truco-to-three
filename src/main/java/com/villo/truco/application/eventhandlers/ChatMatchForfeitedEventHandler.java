package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.application.ports.out.MatchEventContext;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.ports.ChatRepository;
import java.util.Objects;

public final class ChatMatchForfeitedEventHandler
    implements MatchDomainEventHandler<MatchForfeitedEvent> {

    private final ChatRepository chatRepository;
    private final ChatQueryRepository chatQueryRepository;

    public ChatMatchForfeitedEventHandler(final ChatRepository chatRepository,
        final ChatQueryRepository chatQueryRepository) {

        this.chatRepository = Objects.requireNonNull(chatRepository);
        this.chatQueryRepository = Objects.requireNonNull(chatQueryRepository);
    }

    @Override
    public Class<MatchForfeitedEvent> eventType() {

        return MatchForfeitedEvent.class;
    }

    @Override
    public void handle(final MatchForfeitedEvent event, final MatchEventContext context) {

        ChatLifecycleSupport.deleteChat(this.chatQueryRepository, this.chatRepository,
            ChatParentType.MATCH, context.matchId().value().toString());
    }

}
