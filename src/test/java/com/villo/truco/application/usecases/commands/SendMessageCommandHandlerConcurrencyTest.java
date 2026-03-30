package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.commands.SendMessageCommand;
import com.villo.truco.application.ports.in.SendMessageUseCase;
import com.villo.truco.application.ports.in.UseCase;
import com.villo.truco.domain.model.chat.Chat;
import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.ports.ChatEventNotifier;
import com.villo.truco.infrastructure.persistence.repositories.InMemoryChatRepositoryAdapter;
import com.villo.truco.infrastructure.pipeline.OptimisticLockRetryBehavior;
import com.villo.truco.infrastructure.pipeline.UseCasePipeline;
import com.villo.truco.support.TestTransactionRunner;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

@DisplayName("SendMessageCommandHandler - concurrency")
class SendMessageCommandHandlerConcurrencyTest {

  private static void awaitLatch(final CountDownLatch latch) {

    try {
      latch.await();
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Latch interrupted", e);
    }
  }

  @RepeatedTest(30)
  @DisplayName("dos mensajes concurrentes no pierden actualizaciones")
  void concurrentMessagesDoNotLoseUpdates() throws InterruptedException {

    final var repository = new InMemoryChatRepositoryAdapter();
    final var chat = Chat.create(ChatParentType.MATCH, "parent-123",
        Set.of(com.villo.truco.domain.shared.valueobjects.PlayerId.generate(),
            com.villo.truco.domain.shared.valueobjects.PlayerId.generate()));
    repository.save(chat);
    chat.clearDomainEvents();

    final ChatEventNotifier notifier = events -> {
    };
    final var pipeline = new UseCasePipeline(
        List.of(new OptimisticLockRetryBehavior(3, Duration.ZERO)));
    final var rawHandler = new SendMessageCommandHandler(new ChatResolver(repository), repository,
        notifier);
    final UseCase<SendMessageCommand, ChatId> transactionalHandler = command -> TestTransactionRunner.inTransaction(
        () -> rawHandler.handle(command));
    final SendMessageUseCase handler = pipeline.wrap(transactionalHandler)::handle;

    final var players = chat.getParticipants().stream().toList();
    final var latch = new CountDownLatch(1);

    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      executor.submit(() -> {
        awaitLatch(latch);
        handler.handle(new SendMessageCommand(chat.getId(), players.get(0), "hola"));
      });
      executor.submit(() -> {
        awaitLatch(latch);
        handler.handle(new SendMessageCommand(chat.getId(), players.get(1), "chau"));
      });

      latch.countDown();
      executor.shutdown();
      assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }

    final var stored = repository.findById(chat.getId()).orElseThrow();
    assertThat(stored.toReadView().messages()).hasSize(2);
    assertThat(stored.toReadView().messages().stream().map(message -> message.content())
        .toList()).containsExactlyInAnyOrder("hola", "chau");
  }

}
