package com.villo.truco.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import com.villo.truco.infrastructure.events.InProcessApplicationEventPublisher;
import com.villo.truco.infrastructure.events.TransactionalApplicationEventPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class ApplicationEventConfigurationTest {

  @Test
  void applicationEventPublisherIsTransactionalWrapper() {

    final var configuration = new ApplicationEventConfiguration(mock(SimpMessagingTemplate.class),
        mock(EventNotifierHealthRegistry.class), mock(MeterRegistry.class));

    final var inProcess = configuration.inProcessApplicationEventPublisher(List.of());
    final var publisher = configuration.applicationEventPublisher(inProcess);

    assertThat(inProcess).isInstanceOf(InProcessApplicationEventPublisher.class);
    assertThat(publisher).isInstanceOf(TransactionalApplicationEventPublisher.class);
    assertThat(publisher).isInstanceOf(ApplicationEventPublisher.class);
  }

  @Test
  void buildsChatApplicationEventBeans() {

    final var configuration = new ApplicationEventConfiguration(mock(SimpMessagingTemplate.class),
        mock(EventNotifierHealthRegistry.class), mock(MeterRegistry.class));

    assertThat(configuration.chatEventMapper(mock(PublicActorResolver.class))).isNotNull();
    assertThat(configuration.stompChatNotificationHandler()).isNotNull();
    assertThat(configuration.chatEventMetricsHandler()).isNotNull();
  }

}
