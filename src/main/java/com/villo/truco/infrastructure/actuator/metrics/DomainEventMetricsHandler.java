package com.villo.truco.infrastructure.actuator.metrics;

import com.villo.truco.application.ports.out.DomainEventHandler;
import com.villo.truco.domain.shared.DomainEventBase;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;

public abstract class DomainEventMetricsHandler<C>
    implements DomainEventHandler<DomainEventBase, C> {

    private final MeterRegistry meterRegistry;
    private final String metricName;

    protected DomainEventMetricsHandler(final MeterRegistry meterRegistry,
        final String metricName) {

        this.meterRegistry = Objects.requireNonNull(meterRegistry);
        this.metricName = Objects.requireNonNull(metricName);
    }

    @Override
    public final Class<DomainEventBase> eventType() {

        return DomainEventBase.class;
    }

    @Override
    public final void handle(final DomainEventBase event, final C context) {

        this.meterRegistry.counter(this.metricName, "eventType", event.getEventType()).increment();
    }

}
