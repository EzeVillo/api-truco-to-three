package com.villo.truco.infrastructure.config;

import com.villo.truco.application.eventhandlers.ChatEventMapper;
import com.villo.truco.application.eventhandlers.ChatNotificationEventTranslator;
import com.villo.truco.application.eventhandlers.CompetitionDomainEventTranslator;
import com.villo.truco.application.eventhandlers.CupEventMapper;
import com.villo.truco.application.eventhandlers.CupNotificationEventTranslator;
import com.villo.truco.application.eventhandlers.LeagueEventMapper;
import com.villo.truco.application.eventhandlers.LeagueNotificationEventTranslator;
import com.villo.truco.application.eventhandlers.MatchEventMapper;
import com.villo.truco.application.eventhandlers.MatchNotificationEventTranslator;
import com.villo.truco.application.eventhandlers.MatchRecipientResolver;
import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.application.ports.out.ApplicationEventHandler;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import com.villo.truco.infrastructure.actuator.metrics.ChatEventMetricsEventHandler;
import com.villo.truco.infrastructure.actuator.metrics.CupEventMetricsEventHandler;
import com.villo.truco.infrastructure.actuator.metrics.LeagueEventMetricsEventHandler;
import com.villo.truco.infrastructure.actuator.metrics.MatchEventMetricsEventHandler;
import com.villo.truco.infrastructure.events.InProcessApplicationEventPublisher;
import com.villo.truco.infrastructure.events.TransactionalApplicationEventPublisher;
import com.villo.truco.infrastructure.websocket.StompChatNotificationHandler;
import com.villo.truco.infrastructure.websocket.StompCupNotificationHandler;
import com.villo.truco.infrastructure.websocket.StompLeagueNotificationHandler;
import com.villo.truco.infrastructure.websocket.StompMatchNotificationHandler;
import com.villo.truco.infrastructure.websocket.StompPublicCupLobbyNotificationHandler;
import com.villo.truco.infrastructure.websocket.StompPublicLeagueLobbyNotificationHandler;
import com.villo.truco.infrastructure.websocket.StompPublicMatchLobbyNotificationHandler;
import com.villo.truco.infrastructure.websocket.StompSpectatorCountHandler;
import com.villo.truco.infrastructure.websocket.StompSpectatorNotificationHandler;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Configuration
public class ApplicationEventConfiguration {

  private final SimpMessagingTemplate messagingTemplate;
  private final EventNotifierHealthRegistry eventNotifierHealthRegistry;
  private final MeterRegistry meterRegistry;

  public ApplicationEventConfiguration(final SimpMessagingTemplate messagingTemplate,
      final EventNotifierHealthRegistry eventNotifierHealthRegistry,
      final MeterRegistry meterRegistry) {

    this.messagingTemplate = messagingTemplate;
    this.eventNotifierHealthRegistry = eventNotifierHealthRegistry;
    this.meterRegistry = meterRegistry;
  }

  @Bean
  StompChatNotificationHandler stompChatNotificationHandler() {

    return new StompChatNotificationHandler(this.messagingTemplate,
        this.eventNotifierHealthRegistry);
  }

  @Bean
  StompMatchNotificationHandler stompMatchNotificationHandler() {

    return new StompMatchNotificationHandler(this.messagingTemplate,
        this.eventNotifierHealthRegistry);
  }

  @Bean
  StompCupNotificationHandler stompCupNotificationHandler() {

    return new StompCupNotificationHandler(this.messagingTemplate,
        this.eventNotifierHealthRegistry);
  }

  @Bean
  StompLeagueNotificationHandler stompLeagueNotificationHandler() {

    return new StompLeagueNotificationHandler(this.messagingTemplate,
        this.eventNotifierHealthRegistry);
  }

  @Bean
  StompPublicMatchLobbyNotificationHandler stompPublicMatchLobbyNotificationHandler() {

    return new StompPublicMatchLobbyNotificationHandler(this.messagingTemplate,
        this.eventNotifierHealthRegistry);
  }

  @Bean
  StompPublicCupLobbyNotificationHandler stompPublicCupLobbyNotificationHandler() {

    return new StompPublicCupLobbyNotificationHandler(this.messagingTemplate,
        this.eventNotifierHealthRegistry);
  }

  @Bean
  StompPublicLeagueLobbyNotificationHandler stompPublicLeagueLobbyNotificationHandler() {

    return new StompPublicLeagueLobbyNotificationHandler(this.messagingTemplate,
        this.eventNotifierHealthRegistry);
  }

  @Bean
  ChatEventMetricsEventHandler chatEventMetricsHandler() {

    return new ChatEventMetricsEventHandler(this.meterRegistry);
  }

  @Bean
  MatchEventMetricsEventHandler matchEventMetricsHandler() {

    return new MatchEventMetricsEventHandler(this.meterRegistry);
  }

  @Bean
  CupEventMetricsEventHandler cupEventMetricsHandler() {

    return new CupEventMetricsEventHandler(this.meterRegistry);
  }

  @Bean
  LeagueEventMetricsEventHandler leagueEventMetricsHandler() {

    return new LeagueEventMetricsEventHandler(this.meterRegistry);
  }

  @Bean
  StompSpectatorNotificationHandler stompSpectatorNotificationHandler() {

    return new StompSpectatorNotificationHandler(this.messagingTemplate,
        this.eventNotifierHealthRegistry);
  }

  @Bean
  StompSpectatorCountHandler stompSpectatorCountHandler() {

    return new StompSpectatorCountHandler(this.messagingTemplate, this.eventNotifierHealthRegistry);
  }

  @Bean
  ChatEventMapper chatEventMapper(final PublicActorResolver publicActorResolver) {

    return new ChatEventMapper(publicActorResolver);
  }

  @Bean
  MatchEventMapper matchEventMapper() {

    return new MatchEventMapper();
  }

  @Bean
  MatchRecipientResolver matchRecipientResolver() {

    return new MatchRecipientResolver();
  }

  @Bean
  CupEventMapper cupEventMapper(final PublicActorResolver publicActorResolver) {

    return new CupEventMapper(publicActorResolver);
  }

  @Bean
  LeagueEventMapper leagueEventMapper(final PublicActorResolver publicActorResolver) {

    return new LeagueEventMapper(publicActorResolver);
  }

  @Bean
  ChatNotificationEventTranslator chatNotificationTranslator(
      final ApplicationEventPublisher publisher, final PublicActorResolver publicActorResolver) {

    return new ChatNotificationEventTranslator(chatEventMapper(publicActorResolver), publisher);
  }

  @Bean
  CompetitionDomainEventTranslator competitionDomainEventTranslator(
      final ApplicationEventPublisher publisher) {

    return new CompetitionDomainEventTranslator(publisher);
  }

  @Bean
  MatchNotificationEventTranslator matchNotificationTranslator(
      final ApplicationEventPublisher publisher) {

    return new MatchNotificationEventTranslator(matchEventMapper(), matchRecipientResolver(),
        publisher);
  }

  @Bean
  CupNotificationEventTranslator cupNotificationTranslator(
      final ApplicationEventPublisher publisher, final PublicActorResolver publicActorResolver) {

    return new CupNotificationEventTranslator(cupEventMapper(publicActorResolver), publisher);
  }

  @Bean
  LeagueNotificationEventTranslator leagueNotificationTranslator(
      final ApplicationEventPublisher publisher, final PublicActorResolver publicActorResolver) {

    return new LeagueNotificationEventTranslator(leagueEventMapper(publicActorResolver), publisher);
  }

  @Bean
  InProcessApplicationEventPublisher inProcessApplicationEventPublisher(
      final List<ApplicationEventHandler<?>> handlers) {

    return new InProcessApplicationEventPublisher(handlers);
  }

  @Bean
  @Primary
  ApplicationEventPublisher applicationEventPublisher(
      final InProcessApplicationEventPublisher delegate) {

    return new TransactionalApplicationEventPublisher(delegate);
  }

}
