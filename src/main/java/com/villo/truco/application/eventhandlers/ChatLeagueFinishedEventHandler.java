package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.LeagueDomainEventHandler;
import com.villo.truco.application.ports.out.LeagueEventContext;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.model.league.events.LeagueFinishedEvent;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.ports.ChatRepository;
import java.util.Objects;

public final class ChatLeagueFinishedEventHandler
    implements LeagueDomainEventHandler<LeagueFinishedEvent> {

    private final ChatRepository chatRepository;
    private final ChatQueryRepository chatQueryRepository;

    public ChatLeagueFinishedEventHandler(final ChatRepository chatRepository,
        final ChatQueryRepository chatQueryRepository) {

        this.chatRepository = Objects.requireNonNull(chatRepository);
        this.chatQueryRepository = Objects.requireNonNull(chatQueryRepository);
    }

    @Override
    public Class<LeagueFinishedEvent> eventType() {

        return LeagueFinishedEvent.class;
    }

    @Override
    public void handle(final LeagueFinishedEvent event, final LeagueEventContext context) {

        ChatLifecycleSupport.deleteChat(this.chatQueryRepository, this.chatRepository,
            ChatParentType.LEAGUE, context.leagueId().value().toString());
    }

}
