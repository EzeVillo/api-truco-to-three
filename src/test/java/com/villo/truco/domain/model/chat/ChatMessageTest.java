package com.villo.truco.domain.model.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.domain.model.chat.events.MessageSentEvent;
import com.villo.truco.domain.model.chat.exceptions.ChatMessageEmptyException;
import com.villo.truco.domain.model.chat.exceptions.ChatMessageTooLongException;
import com.villo.truco.domain.model.chat.valueobjects.ChatMessageId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ChatMessageTest {

  private final PlayerId sender = PlayerId.generate();

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    @DisplayName("should create message and emit MessageSentEvent")
    void create_validContent_emitsEvent() {

      final var now = Instant.now();

      final var message = ChatMessage.create(sender, "Hello", now);
      final var readView = message.toReadView();

      assertThat(message.getId()).isNotNull();
      assertThat(readView.senderId()).isEqualTo(sender);
      assertThat(readView.content()).isEqualTo("Hello");
      assertThat(readView.sentAt()).isEqualTo(now);
      assertThat(message.getDomainEvents()).hasSize(1);
      assertThat(message.getDomainEvents().get(0)).isInstanceOf(MessageSentEvent.class);

      final var event = (MessageSentEvent) message.getDomainEvents().get(0);
      assertThat(event.getSenderId()).isEqualTo(sender);
      assertThat(event.getContent()).isEqualTo("Hello");
      assertThat(event.getSentAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("should reject null content")
    void create_nullContent_throws() {

      assertThatThrownBy(() -> ChatMessage.create(sender, null, Instant.now())).isInstanceOf(
          ChatMessageEmptyException.class);
    }

    @Test
    @DisplayName("should reject empty content")
    void create_emptyContent_throws() {

      assertThatThrownBy(() -> ChatMessage.create(sender, "", Instant.now())).isInstanceOf(
          ChatMessageEmptyException.class);
    }

    @Test
    @DisplayName("should reject blank content")
    void create_blankContent_throws() {

      assertThatThrownBy(() -> ChatMessage.create(sender, "   ", Instant.now())).isInstanceOf(
          ChatMessageEmptyException.class);
    }

    @Test
    @DisplayName("should reject content exceeding max length")
    void create_tooLongContent_throws() {

      final var longContent = "x".repeat(ChatMessage.MAX_CONTENT_LENGTH + 1);

      assertThatThrownBy(() -> ChatMessage.create(sender, longContent, Instant.now())).isInstanceOf(
          ChatMessageTooLongException.class);
    }

    @Test
    @DisplayName("should accept content at exactly max length")
    void create_exactMaxLength_succeeds() {

      final var content = "x".repeat(ChatMessage.MAX_CONTENT_LENGTH);

      final var message = ChatMessage.create(sender, content, Instant.now());

      assertThat(message.toReadView().content()).hasSize(ChatMessage.MAX_CONTENT_LENGTH);
    }

  }

  @Nested
  @DisplayName("reconstruct")
  class Reconstruct {

    @Test
    @DisplayName("should reconstruct without emitting events")
    void reconstruct_noEvents() {

      final var id = ChatMessageId.generate();
      final var now = Instant.now();

      final var message = ChatMessage.reconstruct(id, sender, "Hello", now);
      final var snapshot = message.toSnapshot();

      assertThat(message.getId()).isEqualTo(id);
      assertThat(snapshot.senderId()).isEqualTo(sender);
      assertThat(snapshot.content()).isEqualTo("Hello");
      assertThat(snapshot.sentAt()).isEqualTo(now);
      assertThat(message.getDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("should reconstruct without validating content")
    void reconstruct_noValidation() {

      final var longContent = "x".repeat(ChatMessage.MAX_CONTENT_LENGTH + 100);

      final var message = ChatMessage.reconstruct(ChatMessageId.generate(), sender, longContent,
          Instant.now());

      assertThat(message.toSnapshot().content()).isEqualTo(longContent);
      assertThat(message.getDomainEvents()).isEmpty();
    }

  }

}
