package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.events.ApplicationEvent;
import com.villo.truco.application.events.ChatEventNotification;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.domain.model.chat.events.ChatCreatedEvent;
import com.villo.truco.domain.model.chat.events.ChatEventEnvelope;
import com.villo.truco.domain.model.chat.events.MessageSentEvent;
import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ChatNotificationTranslator")
class ChatNotificationEventTranslatorTest {

  private final List<ApplicationEvent> published = new ArrayList<>();
  private final ApplicationEventPublisher publisher = published::add;
  private final ChatNotificationEventTranslator translator = new ChatNotificationEventTranslator(
      new ChatEventMapper(), publisher);

  @Test
  @DisplayName("ChatCreatedEvent -> publica ChatEventNotification con payload de parent")
  void chatCreatedPublishesNotification() {

    final var chatId = ChatId.generate();
    final var recipients = List.of(PlayerId.generate(), PlayerId.generate());
    final var event = new ChatCreatedEvent(chatId, recipients, ChatParentType.MATCH, "parent-123");

    translator.handle(event);

    assertThat(published).hasSize(1);
    final var notification = (ChatEventNotification) published.getFirst();
    assertThat(notification.chatId()).isEqualTo(chatId);
    assertThat(notification.recipients()).containsExactlyElementsOf(recipients);
    assertThat(notification.eventType()).isEqualTo("CHAT_CREATED");
    assertThat(notification.payload()).containsEntry("parentType", "MATCH")
        .containsEntry("parentId", "parent-123");
  }

  @Test
  @DisplayName("ChatEventEnvelope(MessageSentEvent) -> unwrap y publica payload del mensaje")
  void messageEnvelopePublishesNotification() {

    final var chatId = ChatId.generate();
    final var sender = PlayerId.generate();
    final var other = PlayerId.generate();
    final var recipients = List.of(sender, other);
    final var inner = new MessageSentEvent(sender, "hola", Instant.ofEpochMilli(1234L));
    final var event = new ChatEventEnvelope(chatId, recipients, inner);

    translator.handle(event);

    assertThat(published).hasSize(1);
    final var notification = (ChatEventNotification) published.getFirst();
    assertThat(notification.chatId()).isEqualTo(chatId);
    assertThat(notification.recipients()).containsExactlyElementsOf(recipients);
    assertThat(notification.eventType()).isEqualTo("MESSAGE_SENT");
    assertThat(notification.timestamp()).isEqualTo(inner.getTimestamp());
    assertThat(notification.payload()).containsEntry("senderId", sender.value().toString())
        .containsEntry("content", "hola").containsEntry("sentAt", 1234L);
  }

}
