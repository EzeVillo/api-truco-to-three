package com.villo.truco.infrastructure.scheduler;

import com.villo.truco.application.ports.out.timeout.EntityType;
import com.villo.truco.application.ports.out.timeout.TimeoutKey;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class TimeoutMetrics {

  private final MeterRegistry registry;
  private final Map<EntityType, AtomicInteger> pendingCounters = new ConcurrentHashMap<>();

  public TimeoutMetrics(final MeterRegistry registry) {

    this.registry = registry;
    for (final EntityType type : EntityType.values()) {
      final var counter = new AtomicInteger(0);
      pendingCounters.put(type, counter);
      Gauge.builder("truco.timeout.pending", counter, AtomicInteger::get)
          .tag("entityType", type.name().toLowerCase()).register(registry);
    }
  }

  public void recordScheduled(final TimeoutKey key) {

    Counter.builder("truco.timeout.scheduled").tag("entityType", key.type().name().toLowerCase())
        .register(registry).increment();
    pendingCounters.get(key.type()).incrementAndGet();
  }

  public void recordCancelled(final TimeoutKey key, final String reason) {

    Counter.builder("truco.timeout.cancelled").tag("entityType", key.type().name().toLowerCase())
        .tag("reason", reason).register(registry).increment();
    pendingCounters.get(key.type()).decrementAndGet();
  }

  public void recordFired(final TimeoutKey key, final String outcome, final long lagMs) {

    Counter.builder("truco.timeout.fired").tag("entityType", key.type().name().toLowerCase())
        .tag("outcome", outcome).register(registry).increment();
    DistributionSummary.builder("truco.timeout.lag")
        .tag("entityType", key.type().name().toLowerCase()).baseUnit("ms").register(registry)
        .record(lagMs);
    pendingCounters.get(key.type()).decrementAndGet();
  }

  public Timer.Sample startReconcile() {

    return Timer.start(registry);
  }

  public void stopReconcile(final Timer.Sample sample, final String phase) {

    sample.stop(
        Timer.builder("truco.timeout.reconcile.duration").tag("phase", phase).register(registry));
  }

}
