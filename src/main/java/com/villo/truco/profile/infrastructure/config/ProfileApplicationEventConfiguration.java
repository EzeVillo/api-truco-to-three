package com.villo.truco.profile.infrastructure.config;

import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import com.villo.truco.profile.application.eventhandlers.ProfileNotificationEventTranslator;
import com.villo.truco.profile.domain.ports.ProfileEventNotifier;
import com.villo.truco.profile.infrastructure.events.CompositeProfileEventNotifier;
import com.villo.truco.profile.infrastructure.websocket.StompProfileNotificationHandler;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Configuration
public class ProfileApplicationEventConfiguration {

  private final SimpMessagingTemplate messagingTemplate;
  private final EventNotifierHealthRegistry eventNotifierHealthRegistry;

  public ProfileApplicationEventConfiguration(final SimpMessagingTemplate messagingTemplate,
      final EventNotifierHealthRegistry eventNotifierHealthRegistry) {

    this.messagingTemplate = messagingTemplate;
    this.eventNotifierHealthRegistry = eventNotifierHealthRegistry;
  }

  @Bean
  StompProfileNotificationHandler stompProfileNotificationHandler() {

    return new StompProfileNotificationHandler(this.messagingTemplate,
        this.eventNotifierHealthRegistry);
  }

  @Bean
  ProfileNotificationEventTranslator profileNotificationEventTranslator(
      final ApplicationEventPublisher applicationEventPublisher) {

    return new ProfileNotificationEventTranslator(applicationEventPublisher);
  }

  @Bean
  ProfileEventNotifier profileEventNotifier(
      final ProfileNotificationEventTranslator profileNotificationEventTranslator) {

    return new CompositeProfileEventNotifier(List.of(profileNotificationEventTranslator));
  }
}
