package com.villo.truco.infrastructure.actuator.metrics;

import com.villo.truco.application.events.CupEventNotification;
import com.villo.truco.application.ports.out.ApplicationEventHandler;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;

public final class CupEventMetricsEventHandler implements ApplicationEventHandler<CupEventNotification> {

  private final MeterRegistry meterRegistry;

  public CupEventMetricsEventHandler(final MeterRegistry meterRegistry) {

    this.meterRegistry = Objects.requireNonNull(meterRegistry);
  }

  @Override
  public Class<CupEventNotification> eventType() {

    return CupEventNotification.class;
  }

  @Override
  public void handle(final CupEventNotification notification) {

    this.meterRegistry.counter("truco.cup.domain.events.total", "eventType",
        notification.eventType()).increment();
  }

}
