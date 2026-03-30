package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.LeagueDomainEventHandler;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.model.league.events.LeagueCancelledEvent;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.ports.ChatRepository;
import java.util.Objects;

public final class ChatLeagueCancelledEventHandler implements
    LeagueDomainEventHandler<LeagueCancelledEvent> {

  private final ChatRepository chatRepository;
  private final ChatQueryRepository chatQueryRepository;

  public ChatLeagueCancelledEventHandler(final ChatRepository chatRepository,
      final ChatQueryRepository chatQueryRepository) {

    this.chatRepository = Objects.requireNonNull(chatRepository);
    this.chatQueryRepository = Objects.requireNonNull(chatQueryRepository);
  }

  @Override
  public Class<LeagueCancelledEvent> eventType() {

    return LeagueCancelledEvent.class;
  }

  @Override
  public void handle(final LeagueCancelledEvent event) {

    ChatLifecycleSupport.deleteChat(this.chatQueryRepository, this.chatRepository,
        ChatParentType.LEAGUE, event.getLeagueId().value().toString());
  }

}
