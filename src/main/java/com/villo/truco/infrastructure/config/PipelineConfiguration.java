package com.villo.truco.infrastructure.config;

import com.villo.truco.infrastructure.pipeline.OptimisticLockRetryBehavior;
import com.villo.truco.infrastructure.pipeline.TransactionalBehavior;
import com.villo.truco.infrastructure.pipeline.UseCasePipeline;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class PipelineConfiguration {

  @Bean
  TransactionTemplate transactionTemplate(final PlatformTransactionManager txManager) {

    return new TransactionTemplate(txManager);
  }

  @Bean
  OptimisticLockRetryBehavior optimisticLockRetryBehavior(
      @Value("${truco.retry.max-retries:3}") final int maxRetries,
      @Value("${truco.retry.delay-ms:200}") final long delayMs) {

    return new OptimisticLockRetryBehavior(maxRetries, Duration.ofMillis(delayMs));
  }

  @Bean
  TransactionalBehavior transactionalBehavior(final TransactionTemplate transactionTemplate) {

    return new TransactionalBehavior(transactionTemplate);
  }

  @Bean
  UseCasePipeline retryTransactionalPipeline(
      final OptimisticLockRetryBehavior optimisticLockRetryBehavior,
      final TransactionalBehavior transactionalBehavior) {

    return new UseCasePipeline(List.of(optimisticLockRetryBehavior, transactionalBehavior));
  }

  @Bean
  UseCasePipeline transactionalPipeline(final TransactionalBehavior transactionalBehavior) {

    return new UseCasePipeline(List.of(transactionalBehavior));
  }

}
