package com.villo.truco.infrastructure.scheduler;

import com.villo.truco.application.ports.out.timeout.TimeoutKey;
import com.villo.truco.application.ports.out.timeout.TimeoutScheduler;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Component
public class SpringTimeoutScheduler implements TimeoutScheduler {

  private static final Logger LOGGER = LoggerFactory.getLogger(SpringTimeoutScheduler.class);

  private final TaskScheduler taskScheduler;
  private final Clock clock;
  private final TimeoutMetrics metrics;
  private final ConcurrentHashMap<TimeoutKey, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

  public SpringTimeoutScheduler(
      @Qualifier("timeoutTaskScheduler") final TaskScheduler taskScheduler, final Clock clock,
      final TimeoutMetrics metrics) {

    this.taskScheduler = taskScheduler;
    this.clock = clock;
    this.metrics = metrics;
  }

  @Override
  public void schedule(final TimeoutKey key, final Instant deadline, final Runnable action) {

    final var now = clock.instant();
    final var effectiveDeadline = deadline.isBefore(now) ? now : deadline;
    final long etaSeconds = Math.max(0, effectiveDeadline.getEpochSecond() - now.getEpochSecond());

    futures.compute(key, (k, existing) -> {
      if (existing != null) {
        existing.cancel(false);
        metrics.recordCancelled(key, "reschedule");
      }
      final Runnable wrapped = () -> fireAction(key, deadline, action);
      final var future = taskScheduler.schedule(wrapped, effectiveDeadline);
      LOGGER.info("Timeout programado: entityType={}, entityId={}, deadlineUtc={}, etaSeconds={}",
          key.type(), key.entityId(), deadline, etaSeconds);
      metrics.recordScheduled(key);
      return future;
    });
  }

  @Override
  public void cancel(final TimeoutKey key) {

    futures.compute(key, (k, existing) -> {
      if (existing != null) {
        existing.cancel(false);
        LOGGER.info("Timeout cancelado: entityType={}, entityId={}, razón={}", key.type(),
            key.entityId(), "cancelado-explícitamente");
        metrics.recordCancelled(key, "cancelado-explicitamente");
      }
      return null;
    });
  }

  @Override
  public boolean isPending(final TimeoutKey key) {

    final var future = futures.get(key);
    return future != null && !future.isDone() && !future.isCancelled();
  }

  private void fireAction(final TimeoutKey key, final Instant deadline, final Runnable action) {

    final var now = clock.instant();
    final var lagMs = now.toEpochMilli() - deadline.toEpochMilli();
    futures.remove(key);

    try {
      action.run();
      LOGGER.info("Timeout disparado: entityType={}, entityId={}, lagMs={}", key.type(),
          key.entityId(), lagMs);
      metrics.recordFired(key, "applied", lagMs);
    } catch (final Exception e) {
      LOGGER.error("Error al disparar timeout: entityType={}, entityId={}", key.type(),
          key.entityId(), e);
      metrics.recordFired(key, "skipped", lagMs);
    }
  }

  public boolean isPoolAlive() {

    return !taskScheduler.getClass().getSimpleName().contains("Terminated");
  }

  public int pendingCount() {

    return (int) futures.values().stream().filter(f -> !f.isDone() && !f.isCancelled()).count();
  }

}
