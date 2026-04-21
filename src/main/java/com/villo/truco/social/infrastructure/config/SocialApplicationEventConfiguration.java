package com.villo.truco.social.infrastructure.config;

import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.application.ports.TransactionalRunner;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.application.usecases.commands.JoinTargetDispatcher;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import com.villo.truco.infrastructure.events.CompositeSocialEventNotifier;
import com.villo.truco.social.application.eventhandlers.ResourceInvitationAcceptedJoinEventHandler;
import com.villo.truco.social.application.eventhandlers.ResourceUnjoinableInvitationExpirationHandler;
import com.villo.truco.social.application.eventhandlers.SocialEventMapper;
import com.villo.truco.social.application.eventhandlers.SocialNotificationEventTranslator;
import com.villo.truco.social.domain.ports.ResourceInvitationQueryRepository;
import com.villo.truco.social.domain.ports.ResourceInvitationRepository;
import com.villo.truco.social.domain.ports.SocialEventNotifier;
import com.villo.truco.social.infrastructure.websocket.StompSocialNotificationHandler;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Configuration
public class SocialApplicationEventConfiguration {

  private final SimpMessagingTemplate messagingTemplate;
  private final EventNotifierHealthRegistry eventNotifierHealthRegistry;

  public SocialApplicationEventConfiguration(final SimpMessagingTemplate messagingTemplate,
      final EventNotifierHealthRegistry eventNotifierHealthRegistry) {

    this.messagingTemplate = messagingTemplate;
    this.eventNotifierHealthRegistry = eventNotifierHealthRegistry;
  }

  @Bean
  StompSocialNotificationHandler stompSocialNotificationHandler() {

    return new StompSocialNotificationHandler(this.messagingTemplate,
        this.eventNotifierHealthRegistry);
  }

  @Bean
  SocialEventMapper socialEventMapper(final PublicActorResolver publicActorResolver) {

    return new SocialEventMapper(publicActorResolver);
  }

  @Bean
  SocialNotificationEventTranslator socialNotificationEventTranslator(
      final SocialEventMapper socialEventMapper,
      final ApplicationEventPublisher applicationEventPublisher) {

    return new SocialNotificationEventTranslator(socialEventMapper, applicationEventPublisher);
  }

  @Bean
  ResourceInvitationAcceptedJoinEventHandler resourceInvitationAcceptedJoinEventHandler(
      final JoinTargetDispatcher joinTargetDispatcher) {

    return new ResourceInvitationAcceptedJoinEventHandler(joinTargetDispatcher);
  }

  @Bean
  ResourceUnjoinableInvitationExpirationHandler resourceUnjoinableInvitationExpirationHandler(
      final ResourceInvitationQueryRepository resourceInvitationQueryRepository,
      final ResourceInvitationRepository resourceInvitationRepository,
      @Lazy final SocialEventNotifier socialEventNotifier,
      final TransactionalRunner transactionalRunner) {

    return new ResourceUnjoinableInvitationExpirationHandler(resourceInvitationQueryRepository,
        resourceInvitationRepository, socialEventNotifier, transactionalRunner);
  }

  @Bean
  SocialEventNotifier socialEventNotifier(
      final SocialNotificationEventTranslator socialNotificationEventTranslator,
      final ResourceInvitationAcceptedJoinEventHandler resourceInvitationAcceptedJoinEventHandler) {

    return new CompositeSocialEventNotifier(
        List.of(socialNotificationEventTranslator, resourceInvitationAcceptedJoinEventHandler));
  }

}
