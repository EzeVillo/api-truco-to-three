package com.villo.truco.infrastructure.actuator.metrics;

import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.application.ports.out.MatchEventContext;
import com.villo.truco.domain.shared.DomainEventBase;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;

public final class MatchDomainEventMetricsHandler implements MatchDomainEventHandler<DomainEventBase> {

  private final MeterRegistry meterRegistry;

  public MatchDomainEventMetricsHandler(final MeterRegistry meterRegistry) {

    this.meterRegistry = Objects.requireNonNull(meterRegistry);
  }

  @Override
  public Class<DomainEventBase> eventType() {

    return DomainEventBase.class;
  }

  @Override
  public void handle(final DomainEventBase event, final MatchEventContext context) {

    this.meterRegistry.counter("truco.match.domain.events.total", "eventType", event.getEventType())
        .increment();
  }
}
