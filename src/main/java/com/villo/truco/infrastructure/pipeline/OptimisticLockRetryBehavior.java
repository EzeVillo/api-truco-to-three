package com.villo.truco.infrastructure.pipeline;

import com.villo.truco.application.ports.in.PipelineBehavior;
import com.villo.truco.domain.shared.exceptions.StaleAggregateException;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OptimisticLockRetryBehavior implements PipelineBehavior {

  private static final Logger LOGGER = LoggerFactory.getLogger(OptimisticLockRetryBehavior.class);

  private final int maxRetries;
  private final Duration retryDelay;

  public OptimisticLockRetryBehavior(final int maxRetries, final Duration retryDelay) {

    this.maxRetries = maxRetries;
    this.retryDelay = Objects.requireNonNull(retryDelay);
  }

  @Override
  public <C, R> R handle(final C command, final Supplier<R> next) {

    for (int attempt = 1; attempt <= this.maxRetries; attempt++) {
      try {
        return next.get();
      } catch (final StaleAggregateException e) {
        LOGGER.warn("Optimistic locking conflict on attempt {}/{}", attempt, this.maxRetries);
        if (attempt == this.maxRetries) {
          throw e;
        }
        sleep();
      }
    }
    throw new IllegalStateException("Unreachable");
  }

  private void sleep() {

    try {
      Thread.sleep(this.retryDelay.toMillis());
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Retry interrupted", e);
    }
  }

}
