package com.villo.truco.infrastructure.actuator.health;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class EventNotifierHealthRegistry {

  private final AtomicReference<Instant> lastSuccess = new AtomicReference<>();
  private final AtomicReference<Instant> lastFailure = new AtomicReference<>();
  private final AtomicReference<String> lastFailureMessage = new AtomicReference<>();

  public void recordSuccess() {

    this.lastSuccess.set(Instant.now());
  }

  public void recordFailure(final Throwable throwable) {

    this.lastFailure.set(Instant.now());
    this.lastFailureMessage.set(throwable == null ? "unknown" : throwable.getMessage());
  }

  public Optional<Instant> lastSuccess() {

    return Optional.ofNullable(this.lastSuccess.get());
  }

  public Optional<Instant> lastFailure() {

    return Optional.ofNullable(this.lastFailure.get());
  }

  public Optional<String> lastFailureMessage() {

    return Optional.ofNullable(this.lastFailureMessage.get());
  }

}
