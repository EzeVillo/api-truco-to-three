package com.villo.truco.domain.model.chat.events;

import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.Objects;

public final class MessageSentEvent extends DomainEventBase {

  private final PlayerId senderId;
  private final String content;
  private final Instant sentAt;

  public MessageSentEvent(final PlayerId senderId, final String content, final Instant sentAt) {

    super("MESSAGE_SENT");
    this.senderId = Objects.requireNonNull(senderId);
    this.content = Objects.requireNonNull(content);
    this.sentAt = Objects.requireNonNull(sentAt);
  }

  public PlayerId getSenderId() {

    return this.senderId;
  }

  public String getContent() {

    return this.content;
  }

  public Instant getSentAt() {

    return this.sentAt;
  }

}
