package com.villo.truco.infrastructure.pipeline;

import com.villo.truco.application.ports.in.PipelineBehavior;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionTemplate;

public final class TransactionalBehavior implements PipelineBehavior {

  private final TransactionTemplate transactionTemplate;

  public TransactionalBehavior(final TransactionTemplate transactionTemplate) {

    this.transactionTemplate = Objects.requireNonNull(transactionTemplate);
  }

  @Override
  public <C, R> R handle(final C command, final Supplier<R> next) {

    return Objects.requireNonNull(this.transactionTemplate.execute(status -> next.get()));
  }

}
