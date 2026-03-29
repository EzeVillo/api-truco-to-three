package com.villo.truco.domain.model.chat.events;

import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Objects;

public final class ChatCreatedEvent extends ChatDomainEvent {

  private final ChatParentType parentType;
  private final String parentId;

  public ChatCreatedEvent(final ChatId chatId, final List<PlayerId> participants,
      final ChatParentType parentType, final String parentId) {

    super("CHAT_CREATED", chatId, participants);
    this.parentType = Objects.requireNonNull(parentType);
    this.parentId = Objects.requireNonNull(parentId);
  }

  public ChatParentType getParentType() {

    return this.parentType;
  }

  public String getParentId() {

    return this.parentId;
  }

}
