package com.villo.truco.domain.model.chat;

import com.villo.truco.domain.model.chat.events.MessageSentEvent;
import com.villo.truco.domain.model.chat.exceptions.ChatMessageEmptyException;
import com.villo.truco.domain.model.chat.exceptions.ChatMessageTooLongException;
import com.villo.truco.domain.model.chat.valueobjects.ChatMessageId;
import com.villo.truco.domain.shared.EntityBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.Objects;

final class ChatMessage extends EntityBase<ChatMessageId> {

  static final int MAX_CONTENT_LENGTH = 500;

  private final PlayerId senderId;
  private final String content;
  private final Instant sentAt;

  private ChatMessage(final ChatMessageId id, final PlayerId senderId, final String content,
      final Instant sentAt) {

    super(id);

    this.senderId = Objects.requireNonNull(senderId, "Sender id cannot be null");
    this.content = Objects.requireNonNull(content, "Content cannot be null");
    this.sentAt = Objects.requireNonNull(sentAt, "SentAt cannot be null");
  }

  static ChatMessage create(final PlayerId senderId, final String content, final Instant sentAt) {

    validateContent(content);

    final var message = new ChatMessage(ChatMessageId.generate(), senderId, content, sentAt);
    message.addDomainEvent(new MessageSentEvent(senderId, content, sentAt));
    return message;
  }

  static ChatMessage reconstruct(final ChatMessageId id, final PlayerId senderId,
      final String content, final Instant sentAt) {

    return new ChatMessage(id, senderId, content, sentAt);
  }

  private static void validateContent(final String content) {

    if (content == null || content.isBlank()) {
      throw new ChatMessageEmptyException();
    }
    if (content.length() > MAX_CONTENT_LENGTH) {
      throw new ChatMessageTooLongException(content.length(), MAX_CONTENT_LENGTH);
    }
  }

  ChatMessageView toReadView() {

    return new ChatMessageView(this.id, this.senderId, this.content, this.sentAt);
  }

  ChatMessageSnapshot toSnapshot() {

    return new ChatMessageSnapshot(this.id, this.senderId, this.content, this.sentAt);
  }

}
