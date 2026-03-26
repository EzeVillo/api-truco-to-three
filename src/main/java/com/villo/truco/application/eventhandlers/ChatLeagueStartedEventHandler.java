package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.LeagueDomainEventHandler;
import com.villo.truco.application.ports.out.LeagueEventContext;
import com.villo.truco.domain.model.chat.Chat;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.model.league.events.LeagueStartedEvent;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.ports.ChatRepository;
import java.util.LinkedHashSet;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChatLeagueStartedEventHandler
    implements LeagueDomainEventHandler<LeagueStartedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        ChatLeagueStartedEventHandler.class);

    private final ChatRepository chatRepository;
    private final ChatQueryRepository chatQueryRepository;

    public ChatLeagueStartedEventHandler(final ChatRepository chatRepository,
        final ChatQueryRepository chatQueryRepository) {

        this.chatRepository = Objects.requireNonNull(chatRepository);
        this.chatQueryRepository = Objects.requireNonNull(chatQueryRepository);
    }

    @Override
    public Class<LeagueStartedEvent> eventType() {

        return LeagueStartedEvent.class;
    }

    @Override
    public void handle(final LeagueStartedEvent event, final LeagueEventContext context) {

        final var parentId = context.leagueId().value().toString();
        final var existing = this.chatQueryRepository.findByParentTypeAndParentId(
            ChatParentType.LEAGUE, parentId);

        if (existing.isPresent()) {
            LOGGER.debug("Chat already exists for league={}", parentId);
            return;
        }

        final var participants = new LinkedHashSet<>(context.participants());
        final var chat = Chat.create(ChatParentType.LEAGUE, parentId, participants);
        this.chatRepository.save(chat);
        LOGGER.info("Created chat for league={}", parentId);
    }

}
