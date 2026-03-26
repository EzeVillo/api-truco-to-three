package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.application.ports.out.MatchEventContext;
import com.villo.truco.domain.model.chat.Chat;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.model.match.events.GameStartedEvent;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.ports.ChatRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChatMatchGameStartedEventHandler
    implements MatchDomainEventHandler<GameStartedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        ChatMatchGameStartedEventHandler.class);

    private final ChatRepository chatRepository;
    private final ChatQueryRepository chatQueryRepository;

    public ChatMatchGameStartedEventHandler(final ChatRepository chatRepository,
        final ChatQueryRepository chatQueryRepository) {

        this.chatRepository = Objects.requireNonNull(chatRepository);
        this.chatQueryRepository = Objects.requireNonNull(chatQueryRepository);
    }

    @Override
    public Class<GameStartedEvent> eventType() {

        return GameStartedEvent.class;
    }

    @Override
    public void handle(final GameStartedEvent event, final MatchEventContext context) {

        if (event.getGameNumber() != 1) {
            return;
        }//todo ezevillo

        final var parentId = context.matchId().value().toString();
        final var existing = this.chatQueryRepository.findByParentTypeAndParentId(
            ChatParentType.MATCH, parentId);

        if (existing.isPresent()) {
            LOGGER.debug("Chat already exists for match={}", parentId);
            return;
        }

        final Set<PlayerId> participants = new LinkedHashSet<>();
        participants.add(context.playerOne());
        if (context.playerTwo() != null) {
            participants.add(context.playerTwo());
        }

        final var chat = Chat.create(ChatParentType.MATCH, parentId, participants);
        this.chatRepository.save(chat);
        LOGGER.info("Created chat for match={}", parentId);
    }

}
