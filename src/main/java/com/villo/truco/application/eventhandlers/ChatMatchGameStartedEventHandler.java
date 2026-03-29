package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.model.match.events.GameStartedEvent;
import com.villo.truco.domain.ports.ChatEventNotifier;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.ports.ChatRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class ChatMatchGameStartedEventHandler implements
    MatchDomainEventHandler<GameStartedEvent> {

  private final BotRegistry botRegistry;
  private final ChatRepository chatRepository;
  private final ChatQueryRepository chatQueryRepository;
  private final ChatEventNotifier chatEventNotifier;

  public ChatMatchGameStartedEventHandler(final BotRegistry botRegistry,
      final ChatRepository chatRepository, final ChatQueryRepository chatQueryRepository,
      final ChatEventNotifier chatEventNotifier) {

    this.botRegistry = Objects.requireNonNull(botRegistry);
    this.chatRepository = Objects.requireNonNull(chatRepository);
    this.chatQueryRepository = Objects.requireNonNull(chatQueryRepository);
    this.chatEventNotifier = Objects.requireNonNull(chatEventNotifier);
  }

  @Override
  public Class<GameStartedEvent> eventType() {

    return GameStartedEvent.class;
  }

  @Override
  public void handle(final GameStartedEvent event) {

    if (event.getGameNumber() != 1) {
      return;
    }
    if (this.botRegistry.isBot(event.getPlayerOne()) || (event.getPlayerTwo() != null
        && this.botRegistry.isBot(event.getPlayerTwo()))) {
      return;
    }

    final var parentId = event.getMatchId().value().toString();
    final Set<PlayerId> participants = new LinkedHashSet<>();
    participants.add(event.getPlayerOne());
    if (event.getPlayerTwo() != null) {
      participants.add(event.getPlayerTwo());
    }

    ChatLifecycleSupport.createChat(this.chatQueryRepository, this.chatRepository,
        this.chatEventNotifier, ChatParentType.MATCH, parentId, participants);
  }

}
