package com.villo.truco.infrastructure.actuator.metrics;

import com.villo.truco.application.events.LeagueEventNotification;
import com.villo.truco.application.ports.out.ApplicationEventHandler;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;

public final class LeagueEventMetricsEventHandler implements
    ApplicationEventHandler<LeagueEventNotification> {

  private final MeterRegistry meterRegistry;

  public LeagueEventMetricsEventHandler(final MeterRegistry meterRegistry) {

    this.meterRegistry = Objects.requireNonNull(meterRegistry);
  }

  @Override
  public Class<LeagueEventNotification> eventType() {

    return LeagueEventNotification.class;
  }

  @Override
  public void handle(final LeagueEventNotification notification) {

    this.meterRegistry.counter("truco.league.domain.events.total", "eventType",
        notification.eventType()).increment();
  }

}
