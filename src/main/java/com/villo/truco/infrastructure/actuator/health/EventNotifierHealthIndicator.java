package com.villo.truco.infrastructure.actuator.health;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.stereotype.Component;

@Component("eventNotifier")
public class EventNotifierHealthIndicator implements HealthIndicator {

  private final EventNotifierHealthRegistry healthRegistry;

  public EventNotifierHealthIndicator(final EventNotifierHealthRegistry healthRegistry) {

    this.healthRegistry = healthRegistry;
  }

  @Override
  public Health health() {

    final Map<String, Object> details = new LinkedHashMap<>();

    final var lastSuccess = this.healthRegistry.lastSuccess();
    final var lastFailure = this.healthRegistry.lastFailure();

    lastSuccess.ifPresent(instant -> details.put("lastSuccessAgeMs",
        Duration.between(instant, Instant.now()).toMillis()));
    lastFailure.ifPresent(instant -> details.put("lastFailureAgeMs",
        Duration.between(instant, Instant.now()).toMillis()));
    this.healthRegistry.lastFailureMessage().ifPresent(msg -> details.put("lastFailure", msg));

    if (lastFailure.isPresent() && (lastSuccess.isEmpty() || lastFailure.get()
        .isAfter(lastSuccess.get()))) {
      return Health.status(Status.OUT_OF_SERVICE).withDetails(details).build();
    }

    return Health.up().withDetails(details).build();
  }
}
