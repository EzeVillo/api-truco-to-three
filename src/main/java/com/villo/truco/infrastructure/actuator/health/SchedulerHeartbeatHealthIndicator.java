package com.villo.truco.infrastructure.actuator.health;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.stereotype.Component;

@Component("schedulerHeartbeat")
public class SchedulerHeartbeatHealthIndicator implements HealthIndicator {

  private static final List<String> REQUIRED_SCHEDULERS =
      List.of("match-timeout", "league-timeout", "cup-timeout");

  private final SchedulerHeartbeatRegistry schedulerHeartbeatRegistry;
  private final Duration maxAge;
  private final Instant startedAt;

  public SchedulerHeartbeatHealthIndicator(final SchedulerHeartbeatRegistry schedulerHeartbeatRegistry,
      @Value("${truco.observability.scheduler-heartbeat-max-age-ms:180000}")
      final long maxAgeMs) {

    this.schedulerHeartbeatRegistry = schedulerHeartbeatRegistry;
    this.maxAge = Duration.ofMillis(maxAgeMs);
    this.startedAt = Instant.now();
  }

  @Override
  public Health health() {

    final Instant now = Instant.now();
    final Map<String, Object> details = new LinkedHashMap<>();
    final boolean withinStartupGrace =
      Duration.between(this.startedAt, now).compareTo(this.maxAge) <= 0;

    boolean healthy = true;
    for (final String schedulerName : REQUIRED_SCHEDULERS) {
      final var lastRun = this.schedulerHeartbeatRegistry.lastSuccessfulRun(schedulerName);
      if (lastRun.isEmpty()) {
        if (withinStartupGrace) {
          details.put(schedulerName, "pending-first-run");
        } else {
          healthy = false;
          details.put(schedulerName, "never-ran");
        }
        continue;
      }

      final long ageMs = Duration.between(lastRun.get(), now).toMillis();
      details.put(schedulerName + "AgeMs", ageMs);
      if (ageMs > this.maxAge.toMillis()) {
        healthy = false;
      }
    }

    details.put("maxAllowedAgeMs", this.maxAge.toMillis());

    if (healthy) {
      return Health.up().withDetails(details).build();
    }

    return Health.status(Status.OUT_OF_SERVICE).withDetails(details).build();
  }
}
