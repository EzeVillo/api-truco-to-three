package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.LeagueDomainEventHandler;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.model.league.events.LeagueStartedEvent;
import com.villo.truco.domain.ports.ChatEventNotifier;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.ports.ChatRepository;
import java.util.Objects;

public final class ChatLeagueStartedEventHandler implements
    LeagueDomainEventHandler<LeagueStartedEvent> {

  private final ChatRepository chatRepository;
  private final ChatQueryRepository chatQueryRepository;
  private final ChatEventNotifier chatEventNotifier;

  public ChatLeagueStartedEventHandler(final ChatRepository chatRepository,
      final ChatQueryRepository chatQueryRepository, final ChatEventNotifier chatEventNotifier) {

    this.chatRepository = Objects.requireNonNull(chatRepository);
    this.chatQueryRepository = Objects.requireNonNull(chatQueryRepository);
    this.chatEventNotifier = Objects.requireNonNull(chatEventNotifier);
  }

  @Override
  public Class<LeagueStartedEvent> eventType() {

    return LeagueStartedEvent.class;
  }

  @Override
  public void handle(final LeagueStartedEvent event) {

    final var parentId = event.getLeagueId().value().toString();
    ChatLifecycleSupport.createChat(this.chatQueryRepository, this.chatRepository,
        this.chatEventNotifier, ChatParentType.LEAGUE, parentId, event.getParticipants());
  }

}
