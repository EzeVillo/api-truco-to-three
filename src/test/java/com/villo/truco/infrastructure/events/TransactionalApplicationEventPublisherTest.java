package com.villo.truco.infrastructure.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.events.ApplicationEvent;
import com.villo.truco.application.events.BotTurnRequired;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@DisplayName("TransactionalApplicationEventPublisher")
class TransactionalApplicationEventPublisherTest {

  @AfterEach
  void cleanupSynchronization() {

    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  @DisplayName("publica evento normal inmediatamente sin transaccion")
  void publishesNormalEventImmediatelyWithoutTransaction() {

    final var delegate = new RecordingApplicationEventPublisher();
    final var publisher = new TransactionalApplicationEventPublisher(delegate);
    final var event = new NormalEvent();

    publisher.publish(event);

    assertThat(delegate.events).containsExactly(event);
  }

  @Test
  @DisplayName("publica evento post commit inmediatamente sin transaccion")
  void publishesPostCommitEventImmediatelyWithoutTransaction() {

    final var delegate = new RecordingApplicationEventPublisher();
    final var publisher = new TransactionalApplicationEventPublisher(delegate);
    final var event = new BotTurnRequired(MatchId.generate(), PlayerId.generate());

    publisher.publish(event);

    assertThat(delegate.events).containsExactly(event);
  }

  @Test
  @DisplayName("no difiere evento normal aunque haya transaccion")
  void doesNotDeferNormalEventWhenSynchronizationIsActive() {

    TransactionSynchronizationManager.initSynchronization();

    final var delegate = new RecordingApplicationEventPublisher();
    final var publisher = new TransactionalApplicationEventPublisher(delegate);
    final var event = new NormalEvent();

    publisher.publish(event);

    assertThat(delegate.events).containsExactly(event);
    assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
  }

  @Test
  @DisplayName("difiere evento post commit hasta afterCommit")
  void defersPostCommitEventUntilAfterCommitWhenSynchronizationIsActive() {

    TransactionSynchronizationManager.initSynchronization();

    final var delegate = new RecordingApplicationEventPublisher();
    final var publisher = new TransactionalApplicationEventPublisher(delegate);
    final var event = new BotTurnRequired(MatchId.generate(), PlayerId.generate());

    publisher.publish(event);

    assertThat(delegate.events).isEmpty();
    assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);

    TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());

    assertThat(delegate.events).containsExactly(event);
  }

  private record NormalEvent() implements ApplicationEvent {

  }

  private static final class RecordingApplicationEventPublisher implements
      ApplicationEventPublisher {

    private final List<ApplicationEvent> events = new ArrayList<>();

    @Override
    public void publish(final ApplicationEvent event) {

      this.events.add(event);
    }

  }

}
