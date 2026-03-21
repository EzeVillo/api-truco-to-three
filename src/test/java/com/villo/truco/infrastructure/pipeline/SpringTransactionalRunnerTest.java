package com.villo.truco.infrastructure.pipeline;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

@DisplayName("SpringTransactionalRunner")
class SpringTransactionalRunnerTest {

  @Test
  @DisplayName("run delega en TransactionTemplate")
  void runDelegates() {

    final var template = mock(TransactionTemplate.class);
    final var runner = new SpringTransactionalRunner(template);

    runner.run(() -> {
    });

    verify(template).executeWithoutResult(any());
  }

}
