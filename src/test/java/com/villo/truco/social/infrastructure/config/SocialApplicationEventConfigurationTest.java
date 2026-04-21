package com.villo.truco.social.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.application.usecases.commands.JoinTargetDispatcher;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class SocialApplicationEventConfigurationTest {

  @Test
  void buildsSocialApplicationEventBeans() {

    final var configuration = new SocialApplicationEventConfiguration(
        mock(SimpMessagingTemplate.class), mock(EventNotifierHealthRegistry.class));
    final var mapper = configuration.socialEventMapper(mock(PublicActorResolver.class));
    final var translator = configuration.socialNotificationEventTranslator(mapper,
        mock(ApplicationEventPublisher.class));

    final var joinEventHandler = configuration.resourceInvitationAcceptedJoinEventHandler(
        mock(JoinTargetDispatcher.class));

    assertThat(configuration.stompSocialNotificationHandler()).isNotNull();
    assertThat(mapper).isNotNull();
    assertThat(translator).isNotNull();
    assertThat(joinEventHandler).isNotNull();
    assertThat(configuration.socialEventNotifier(translator, joinEventHandler)).isNotNull();
  }

}
