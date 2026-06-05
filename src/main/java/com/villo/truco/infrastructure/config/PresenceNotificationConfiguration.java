package com.villo.truco.infrastructure.config;

import com.villo.truco.application.eventhandlers.CupPresenceEventTranslator;
import com.villo.truco.application.eventhandlers.LeaguePresenceEventTranslator;
import com.villo.truco.application.eventhandlers.MatchPresenceEventTranslator;
import com.villo.truco.application.eventhandlers.PresenceNotifier;
import com.villo.truco.application.eventhandlers.RematchPresenceEventTranslator;
import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.application.usecases.queries.UserPresenceResolver;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import com.villo.truco.infrastructure.websocket.StompPresenceNotificationHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Wiring del push de presencia en tiempo real (009). Declara el colaborador
 * {@link PresenceNotifier} y los traductores por dominio (que se enchufan en los composites de
 * notificadores existentes), mas el handler STOMP que entrega a {@code /user/queue/presence}. El
 * handler se auto-registra como consumidor de {@code PresenceEventNotification} via la lista de
 * {@code ApplicationEventHandler}.
 */
@Configuration
public class PresenceNotificationConfiguration {

  @Bean
  PresenceNotifier presenceNotifier(final UserPresenceResolver userPresenceResolver,
      @Lazy final ApplicationEventPublisher applicationEventPublisher,
      final BotRegistry botRegistry) {

    return new PresenceNotifier(userPresenceResolver, applicationEventPublisher, botRegistry);
  }

  @Bean
  MatchPresenceEventTranslator matchPresenceEventTranslator(
      final PresenceNotifier presenceNotifier) {

    return new MatchPresenceEventTranslator(presenceNotifier);
  }

  @Bean
  LeaguePresenceEventTranslator leaguePresenceEventTranslator(
      final PresenceNotifier presenceNotifier) {

    return new LeaguePresenceEventTranslator(presenceNotifier);
  }

  @Bean
  CupPresenceEventTranslator cupPresenceEventTranslator(final PresenceNotifier presenceNotifier) {

    return new CupPresenceEventTranslator(presenceNotifier);
  }

  @Bean
  RematchPresenceEventTranslator rematchPresenceEventTranslator(
      final PresenceNotifier presenceNotifier) {

    return new RematchPresenceEventTranslator(presenceNotifier);
  }

  @Bean
  StompPresenceNotificationHandler stompPresenceNotificationHandler(
      final SimpMessagingTemplate messagingTemplate,
      final EventNotifierHealthRegistry eventNotifierHealthRegistry) {

    return new StompPresenceNotificationHandler(messagingTemplate, eventNotifierHealthRegistry);
  }

}
