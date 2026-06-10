package com.villo.truco.infrastructure.actuator.health;

import com.villo.truco.application.ports.out.timeout.EntityType;
import com.villo.truco.infrastructure.scheduler.SpringTimeoutScheduler;
import com.villo.truco.infrastructure.scheduler.TimeoutReconciliationRunner;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.stereotype.Component;

@Component("schedulerHeartbeat")
public class SchedulerHeartbeatHealthIndicator implements HealthIndicator {

  private final TimeoutReconciliationRunner reconciliationRunner;
  private final SpringTimeoutScheduler springTimeoutScheduler;
  private final Duration maxAge;

  public SchedulerHeartbeatHealthIndicator(final TimeoutReconciliationRunner reconciliationRunner,
      final SpringTimeoutScheduler springTimeoutScheduler,
      @Value("${truco.observability.scheduler-heartbeat-max-age-ms:360000}") final long maxAgeMs) {

    this.reconciliationRunner = reconciliationRunner;
    this.springTimeoutScheduler = springTimeoutScheduler;
    this.maxAge = Duration.ofMillis(maxAgeMs);
  }

  @Override
  public Health health() {

    final Instant now = Instant.now();
    final Map<String, Object> details = new LinkedHashMap<>();
    boolean healthy = springTimeoutScheduler.isPoolAlive();

    if (!healthy) {
      details.put("scheduler", "pool-terminated");
    }

    final Instant lastReconciliation = reconciliationRunner.getLastReconciliationAt();
    details.put("lastReconciliationAt", lastReconciliation);

    if (lastReconciliation == null) {
      details.put("safetyNet", "never-ran");
    } else {
      final long safetyNetAgeMs = Duration.between(lastReconciliation, now).toMillis();
      details.put("safetyNetAgeMs", safetyNetAgeMs);
      if (safetyNetAgeMs > maxAge.toMillis()) {
        healthy = false;
      }
    }

    final Map<String, Integer> pendingByType = new LinkedHashMap<>();
    for (final EntityType type : EntityType.values()) {
      pendingByType.put(type.name().toLowerCase(), 0);
    }
    details.put("pendingByType", pendingByType);
    details.put("pendingTotal", springTimeoutScheduler.pendingCount());

    if (healthy) {
      return Health.up().withDetails(details).build();
    }
    return Health.status(Status.OUT_OF_SERVICE).withDetails(details).build();
  }

}
