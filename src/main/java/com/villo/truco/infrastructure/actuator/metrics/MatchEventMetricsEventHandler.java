package com.villo.truco.infrastructure.actuator.metrics;

import com.villo.truco.application.events.MatchEventNotification;
import com.villo.truco.application.ports.out.ApplicationEventHandler;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;

public final class MatchEventMetricsEventHandler implements
    ApplicationEventHandler<MatchEventNotification> {

  private final MeterRegistry meterRegistry;

  public MatchEventMetricsEventHandler(final MeterRegistry meterRegistry) {

    this.meterRegistry = Objects.requireNonNull(meterRegistry);
  }

  @Override
  public Class<MatchEventNotification> eventType() {

    return MatchEventNotification.class;
  }

  @Override
  public void handle(final MatchEventNotification notification) {

    this.meterRegistry.counter("truco.match.domain.events.total", "eventType",
        notification.eventType()).increment();
  }

}
