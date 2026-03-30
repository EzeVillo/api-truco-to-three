package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.ports.ChatRepository;
import java.util.Objects;

public final class ChatMatchFinishedEventHandler implements
    MatchDomainEventHandler<MatchFinishedEvent> {

  private final ChatRepository chatRepository;
  private final ChatQueryRepository chatQueryRepository;

  public ChatMatchFinishedEventHandler(final ChatRepository chatRepository,
      final ChatQueryRepository chatQueryRepository) {

    this.chatRepository = Objects.requireNonNull(chatRepository);
    this.chatQueryRepository = Objects.requireNonNull(chatQueryRepository);
  }

  @Override
  public Class<MatchFinishedEvent> eventType() {

    return MatchFinishedEvent.class;
  }

  @Override
  public void handle(final MatchFinishedEvent event) {

    ChatLifecycleSupport.deleteChat(this.chatQueryRepository, this.chatRepository,
        ChatParentType.MATCH, event.getMatchId().value().toString());
  }

}
