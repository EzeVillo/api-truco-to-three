package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.CupDomainEventHandler;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.model.cup.events.CupStartedEvent;
import com.villo.truco.domain.ports.ChatEventNotifier;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.ports.ChatRepository;
import java.util.Objects;

public final class ChatCupStartedEventHandler implements CupDomainEventHandler<CupStartedEvent> {

  private final ChatRepository chatRepository;
  private final ChatQueryRepository chatQueryRepository;
  private final ChatEventNotifier chatEventNotifier;

  public ChatCupStartedEventHandler(final ChatRepository chatRepository,
      final ChatQueryRepository chatQueryRepository, final ChatEventNotifier chatEventNotifier) {

    this.chatRepository = Objects.requireNonNull(chatRepository);
    this.chatQueryRepository = Objects.requireNonNull(chatQueryRepository);
    this.chatEventNotifier = Objects.requireNonNull(chatEventNotifier);
  }

  @Override
  public Class<CupStartedEvent> eventType() {

    return CupStartedEvent.class;
  }

  @Override
  public void handle(final CupStartedEvent event) {

    final var parentId = event.getCupId().value().toString();
    ChatLifecycleSupport.createChat(this.chatQueryRepository, this.chatRepository,
        this.chatEventNotifier, ChatParentType.CUP, parentId, event.getParticipants());
  }

}
