package com.villo.truco.infrastructure.actuator.health;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class SchedulerHeartbeatRegistry {

  private final Map<String, Instant> successfulRuns = new ConcurrentHashMap<>();

  public void recordSuccessfulRun(final String schedulerName) {

    this.successfulRuns.put(schedulerName, Instant.now());
  }

  public Optional<Instant> lastSuccessfulRun(final String schedulerName) {

    return Optional.ofNullable(this.successfulRuns.get(schedulerName));
  }

}
