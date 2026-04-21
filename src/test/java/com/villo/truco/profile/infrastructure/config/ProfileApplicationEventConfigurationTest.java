package com.villo.truco.profile.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@DisplayName("ProfileApplicationEventConfiguration")
class ProfileApplicationEventConfigurationTest {

  @Test
  @DisplayName("crea beans de eventos de profile")
  void buildsProfileEventBeans() {

    final var configuration = new ProfileApplicationEventConfiguration(
        mock(SimpMessagingTemplate.class), mock(EventNotifierHealthRegistry.class));
    final var translator = configuration.profileNotificationEventTranslator(
        mock(ApplicationEventPublisher.class));

    assertThat(configuration.stompProfileNotificationHandler()).isNotNull();
    assertThat(translator).isNotNull();
    assertThat(configuration.profileEventNotifier(translator)).isNotNull();
  }
}
