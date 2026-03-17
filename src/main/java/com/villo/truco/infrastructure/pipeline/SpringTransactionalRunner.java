package com.villo.truco.infrastructure.pipeline;

import com.villo.truco.application.ports.TransactionalRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class SpringTransactionalRunner implements TransactionalRunner {

  private final TransactionTemplate transactionTemplate;

  public SpringTransactionalRunner(final TransactionTemplate transactionTemplate) {

    this.transactionTemplate = transactionTemplate;
  }

  @Override
  public void run(final Runnable action) {

    this.transactionTemplate.executeWithoutResult(status -> action.run());
  }

}
