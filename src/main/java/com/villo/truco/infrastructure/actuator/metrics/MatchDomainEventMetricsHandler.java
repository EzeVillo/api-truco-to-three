package com.villo.truco.infrastructure.actuator.metrics;

import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.application.ports.out.MatchEventContext;
import com.villo.truco.domain.shared.DomainEventBase;
import io.micrometer.core.instrument.MeterRegistry;

public final class MatchDomainEventMetricsHandler
    extends DomainEventMetricsHandler<MatchEventContext>
    implements MatchDomainEventHandler<DomainEventBase> {

  public MatchDomainEventMetricsHandler(final MeterRegistry meterRegistry) {

    super(meterRegistry, "truco.match.domain.events.total");
  }

}
