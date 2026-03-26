package com.villo.truco.infrastructure.actuator.metrics;

import com.villo.truco.application.ports.out.CupDomainEventHandler;
import com.villo.truco.application.ports.out.CupEventContext;
import com.villo.truco.domain.shared.DomainEventBase;
import io.micrometer.core.instrument.MeterRegistry;

public final class CupDomainEventMetricsHandler
    extends DomainEventMetricsHandler<CupEventContext>
    implements CupDomainEventHandler<DomainEventBase> {

    public CupDomainEventMetricsHandler(final MeterRegistry meterRegistry) {

        super(meterRegistry, "truco.cup.domain.events.total");
    }

}
