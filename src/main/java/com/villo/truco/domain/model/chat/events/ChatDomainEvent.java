package com.villo.truco.domain.model.chat.events;

import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Objects;

public abstract class ChatDomainEvent extends DomainEventBase {

  private final ChatId chatId;
  private final List<PlayerId> participants;

  protected ChatDomainEvent(final String eventType, final ChatId chatId,
      final List<PlayerId> participants) {

    super(eventType);
    this.chatId = Objects.requireNonNull(chatId);
    this.participants = List.copyOf(Objects.requireNonNull(participants));
  }

  public ChatId getChatId() {

    return this.chatId;
  }

  public List<PlayerId> getParticipants() {

    return this.participants;
  }

}
