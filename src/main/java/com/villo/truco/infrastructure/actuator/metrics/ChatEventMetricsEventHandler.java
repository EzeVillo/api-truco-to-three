package com.villo.truco.infrastructure.actuator.metrics;

import com.villo.truco.application.events.ChatEventNotification;
import com.villo.truco.application.ports.out.ApplicationEventHandler;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;

public final class ChatEventMetricsEventHandler implements
    ApplicationEventHandler<ChatEventNotification> {

  private final MeterRegistry meterRegistry;

  public ChatEventMetricsEventHandler(final MeterRegistry meterRegistry) {

    this.meterRegistry = Objects.requireNonNull(meterRegistry);
  }

  @Override
  public Class<ChatEventNotification> eventType() {

    return ChatEventNotification.class;
  }

  @Override
  public void handle(final ChatEventNotification notification) {

    this.meterRegistry.counter("truco.chat.domain.events.total", "eventType",
        notification.eventType()).increment();
  }

}
