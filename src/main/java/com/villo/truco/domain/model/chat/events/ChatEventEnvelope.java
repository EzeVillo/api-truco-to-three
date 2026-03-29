package com.villo.truco.domain.model.chat.events;

import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Objects;

public final class ChatEventEnvelope extends ChatDomainEvent {

  private final DomainEventBase inner;

  public ChatEventEnvelope(final ChatId chatId, final List<PlayerId> participants,
      final DomainEventBase inner) {

    super(Objects.requireNonNull(inner).getEventType(), chatId, participants);
    this.inner = inner;
  }

  public DomainEventBase getInner() {

    return this.inner;
  }

}
