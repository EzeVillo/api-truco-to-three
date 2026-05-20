package com.villo.truco.infrastructure.pipeline;

import com.villo.truco.application.ports.RetryableTransactionalRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class SpringRetryableTransactionalRunner implements RetryableTransactionalRunner {

  private final OptimisticLockRetryBehavior retryBehavior;
  private final TransactionTemplate transactionTemplate;

  public SpringRetryableTransactionalRunner(final OptimisticLockRetryBehavior retryBehavior,
      final TransactionTemplate transactionTemplate) {

    this.retryBehavior = retryBehavior;
    this.transactionTemplate = transactionTemplate;
  }

  @Override
  public void run(final Runnable action) {

    this.retryBehavior.handle(null, () -> {
      this.transactionTemplate.executeWithoutResult(status -> action.run());
      return null;
    });
  }

}
