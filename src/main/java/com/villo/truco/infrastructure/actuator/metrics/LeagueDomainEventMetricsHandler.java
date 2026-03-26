package com.villo.truco.infrastructure.actuator.metrics;

import com.villo.truco.application.ports.out.LeagueDomainEventHandler;
import com.villo.truco.application.ports.out.LeagueEventContext;
import com.villo.truco.domain.shared.DomainEventBase;
import io.micrometer.core.instrument.MeterRegistry;

public final class LeagueDomainEventMetricsHandler
    extends DomainEventMetricsHandler<LeagueEventContext>
    implements LeagueDomainEventHandler<DomainEventBase> {

    public LeagueDomainEventMetricsHandler(final MeterRegistry meterRegistry) {

        super(meterRegistry, "truco.league.domain.events.total");
    }

}
