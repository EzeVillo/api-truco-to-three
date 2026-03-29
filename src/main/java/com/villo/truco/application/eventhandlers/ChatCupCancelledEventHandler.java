package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.CupDomainEventHandler;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.model.cup.events.CupCancelledEvent;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.ports.ChatRepository;
import java.util.Objects;

public final class ChatCupCancelledEventHandler implements
    CupDomainEventHandler<CupCancelledEvent> {

  private final ChatRepository chatRepository;
  private final ChatQueryRepository chatQueryRepository;

  public ChatCupCancelledEventHandler(final ChatRepository chatRepository,
      final ChatQueryRepository chatQueryRepository) {

    this.chatRepository = Objects.requireNonNull(chatRepository);
    this.chatQueryRepository = Objects.requireNonNull(chatQueryRepository);
  }

  @Override
  public Class<CupCancelledEvent> eventType() {

    return CupCancelledEvent.class;
  }

  @Override
  public void handle(final CupCancelledEvent event) {

    ChatLifecycleSupport.deleteChat(this.chatQueryRepository, this.chatRepository,
        ChatParentType.CUP, event.getCupId().value().toString());
  }

}
