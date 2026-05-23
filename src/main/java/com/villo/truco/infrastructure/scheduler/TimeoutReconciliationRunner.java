package com.villo.truco.infrastructure.scheduler;

import com.villo.truco.application.ports.out.timeout.TimeoutReconciliationSource;
import com.villo.truco.application.ports.out.timeout.TimeoutReconciliationSource.TimeoutEntry;
import com.villo.truco.application.ports.out.timeout.TimeoutScheduler;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class TimeoutReconciliationRunner implements ApplicationListener<ApplicationReadyEvent> {

  private static final Logger LOGGER = LoggerFactory.getLogger(TimeoutReconciliationRunner.class);

  private final List<TimeoutReconciliationSource> sources;
  private final TimeoutScheduler timeoutScheduler;
  private final TimeoutMetrics metrics;
  private final Clock clock;

  private volatile Instant lastReconciliationAt;

  public TimeoutReconciliationRunner(final List<TimeoutReconciliationSource> sources,
      final TimeoutScheduler timeoutScheduler, final TimeoutMetrics metrics, final Clock clock) {

    this.sources = sources;
    this.timeoutScheduler = timeoutScheduler;
    this.metrics = metrics;
    this.clock = clock;
  }

  @Override
  public void onApplicationEvent(final ApplicationReadyEvent event) {

    reconcile("startup");
  }

  public void reconcile(final String phase) {

    final var sample = metrics.startReconcile();
    final var now = clock.instant();
    final var total = new AtomicInteger(0);
    final var expired = new AtomicInteger(0);
    final var future = new AtomicInteger(0);

    for (final var source : sources) {
      try (final var stream = source.activeWithDeadline()) {
        stream.forEach(entry -> {
          total.incrementAndGet();
          processEntry(entry, now, expired, future);
        });
      } catch (final Exception e) {
        LOGGER.error("Error en reconciliación de timeouts (fase={})", phase, e);
      }
    }

    metrics.stopReconcile(sample, phase);
    lastReconciliationAt = clock.instant();
    LOGGER.info("Timeout reconciliado al arrancar: total={}, vencidos={}, futuros={}", total.get(),
        expired.get(), future.get());
  }

  private void processEntry(final TimeoutEntry entry, final Instant now,
      final AtomicInteger expired, final AtomicInteger future) {

    if (entry.deadline().isBefore(now) || entry.deadline().equals(now)) {
      expired.incrementAndGet();
    } else {
      future.incrementAndGet();
    }
    timeoutScheduler.schedule(entry.key(), entry.deadline(), entry.action());
  }

  public Instant getLastReconciliationAt() {

    return lastReconciliationAt;
  }

}
