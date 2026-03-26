package com.villo.truco.infrastructure.actuator.metrics;

import com.villo.truco.application.ports.out.ChatDomainEventHandler;
import com.villo.truco.application.ports.out.ChatEventContext;
import com.villo.truco.domain.shared.DomainEventBase;
import io.micrometer.core.instrument.MeterRegistry;

public final class ChatDomainEventMetricsHandler
    extends DomainEventMetricsHandler<ChatEventContext>
    implements ChatDomainEventHandler<DomainEventBase> {

    public ChatDomainEventMetricsHandler(final MeterRegistry meterRegistry) {

        super(meterRegistry, "truco.chat.domain.events.total");
    }

}
