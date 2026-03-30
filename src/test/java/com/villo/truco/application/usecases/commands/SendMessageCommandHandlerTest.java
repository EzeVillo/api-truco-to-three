package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.application.commands.SendMessageCommand;
import com.villo.truco.domain.model.chat.Chat;
import com.villo.truco.domain.model.chat.events.ChatDomainEvent;
import com.villo.truco.domain.model.chat.events.ChatEventEnvelope;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.ports.ChatEventNotifier;
import com.villo.truco.domain.ports.ChatRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.repositories.InMemoryChatRepositoryAdapter;
import com.villo.truco.support.TestTransactionRunner;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("SendMessageCommandHandler")
class SendMessageCommandHandlerTest {

  @Test
  @DisplayName("publica ChatDomainEvent tipado y limpia domain events")
  void publishesTypedChatDomainEventsAndClearsThem() {

    final var sender = PlayerId.generate();
    final var other = PlayerId.generate();
    final var chat = Chat.create(ChatParentType.MATCH, "parent-123",
        new LinkedHashSet<>(List.of(sender, other)));
    chat.clearDomainEvents();

    final var chatResolver = mock(ChatResolver.class);
    final var chatRepository = mock(ChatRepository.class);
    final var chatEventNotifier = mock(ChatEventNotifier.class);
    final var handler = new SendMessageCommandHandler(chatResolver, chatRepository,
        chatEventNotifier);
    final var command = new SendMessageCommand(chat.getId(), sender, "hola");

    when(chatResolver.resolve(chat.getId())).thenReturn(chat);

    handler.handle(command);

    verify(chatRepository).save(chat);
    final var captor = ArgumentCaptor.forClass(List.class);
    verify(chatEventNotifier).publishDomainEvents(captor.capture());

    @SuppressWarnings("unchecked") final var publishedEvents = (List<ChatDomainEvent>) captor.getValue();
    assertThat(publishedEvents).hasSize(1);
    assertThat(publishedEvents.getFirst()).isInstanceOf(ChatEventEnvelope.class);
    assertThat(chat.getChatDomainEvents()).isEmpty();
  }

  @Test
  @DisplayName("restaura el chat si falla la publicacion")
  void restoresChatStateWhenPublishingFails() {

    final var sender = PlayerId.generate();
    final var other = PlayerId.generate();
    final var repository = new InMemoryChatRepositoryAdapter();
    final var chat = Chat.create(ChatParentType.MATCH, "parent-123",
        new LinkedHashSet<>(List.of(sender, other)));
    repository.save(chat);
    chat.clearDomainEvents();

    final var chatResolver = new ChatResolver(repository);
    final ChatEventNotifier failingNotifier = events -> {
      throw new IllegalStateException("ws failed");
    };
    final var handler = new SendMessageCommandHandler(chatResolver, repository, failingNotifier);

    assertThatThrownBy(() -> TestTransactionRunner.inTransaction(
        () -> handler.handle(new SendMessageCommand(chat.getId(), sender, "hola")))).isInstanceOf(
        IllegalStateException.class).hasMessage("ws failed");

    final var stored = repository.findById(chat.getId()).orElseThrow();
    assertThat(stored.toReadView().messages()).isEmpty();
  }

}
