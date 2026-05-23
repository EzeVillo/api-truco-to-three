package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.timeout.EntityType;
import com.villo.truco.application.ports.out.timeout.TimeoutKey;
import com.villo.truco.application.ports.out.timeout.TimeoutScheduler;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public abstract class AbstractTimeoutEventHandler {

  private final TimeoutScheduler timeoutScheduler;
  private final TimeoutActionDispatcher dispatcher;

  protected AbstractTimeoutEventHandler(final TimeoutScheduler timeoutScheduler,
      final TimeoutActionDispatcher dispatcher) {

    this.timeoutScheduler = Objects.requireNonNull(timeoutScheduler);
    this.dispatcher = Objects.requireNonNull(dispatcher);
  }

  protected void scheduleTimeout(final EntityType type, final String entityId,
      final Instant deadline) {

    final TimeoutKey key = TimeoutKey.of(type, entityId);
    timeoutScheduler.schedule(key, deadline, dispatcher.buildAction(key));
  }

  protected void scheduleTimeoutFromNow(final EntityType type, final String entityId,
      final Duration ttl) {

    scheduleTimeout(type, entityId, Instant.now().plus(ttl));
  }

  protected void cancelTimeout(final EntityType type, final String entityId) {

    timeoutScheduler.cancel(TimeoutKey.of(type, entityId));
  }

}
