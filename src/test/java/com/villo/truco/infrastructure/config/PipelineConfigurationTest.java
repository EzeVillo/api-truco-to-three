package com.villo.truco.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

class PipelineConfigurationTest {

  @Test
  void buildsPipelineBeans() {

    final var configuration = new PipelineConfiguration();
    final var retry = configuration.optimisticLockRetryBehavior(2, 1);
    final var transactional = configuration.transactionalBehavior(mock(TransactionTemplate.class));

    assertThat(retry).isNotNull();
    assertThat(transactional).isNotNull();
    assertThat(configuration.retryTransactionalPipeline(retry, transactional)).isNotNull();
  }

}
