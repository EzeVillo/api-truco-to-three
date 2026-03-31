package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.model.match.events.MatchAbandonedEvent;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.ports.ChatRepository;
import java.util.Objects;

public final class ChatMatchAbandonedEventHandler implements
    MatchDomainEventHandler<MatchAbandonedEvent> {

  private final ChatRepository chatRepository;
  private final ChatQueryRepository chatQueryRepository;

  public ChatMatchAbandonedEventHandler(final ChatRepository chatRepository,
      final ChatQueryRepository chatQueryRepository) {

    this.chatRepository = Objects.requireNonNull(chatRepository);
    this.chatQueryRepository = Objects.requireNonNull(chatQueryRepository);
  }

  @Override
  public Class<MatchAbandonedEvent> eventType() {

    return MatchAbandonedEvent.class;
  }

  @Override
  public void handle(final MatchAbandonedEvent event) {

    ChatLifecycleSupport.deleteChat(this.chatQueryRepository, this.chatRepository,
        ChatParentType.MATCH, event.getMatchId().value().toString());
  }

}
