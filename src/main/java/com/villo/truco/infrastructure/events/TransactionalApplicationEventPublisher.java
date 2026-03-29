package com.villo.truco.infrastructure.events;

import com.villo.truco.application.events.ApplicationEvent;
import com.villo.truco.application.events.PostCommitApplicationEvent;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import java.util.Objects;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class TransactionalApplicationEventPublisher implements ApplicationEventPublisher {

  private final ApplicationEventPublisher delegate;

  public TransactionalApplicationEventPublisher(final ApplicationEventPublisher delegate) {

    this.delegate = Objects.requireNonNull(delegate);
  }

  @Override
  public void publish(final ApplicationEvent event) {

    Objects.requireNonNull(event);

    if (event instanceof PostCommitApplicationEvent
        && TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {

          TransactionalApplicationEventPublisher.this.delegate.publish(event);
        }
      });
      return;
    }

    this.delegate.publish(event);
  }

}
