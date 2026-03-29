package com.villo.truco.infrastructure.config;

import com.villo.truco.application.eventhandlers.CompetitionDomainEventTranslator;
import com.villo.truco.application.eventhandlers.CupEventMapper;
import com.villo.truco.application.eventhandlers.CupNotificationEventTranslator;
import com.villo.truco.application.eventhandlers.LeagueEventMapper;
import com.villo.truco.application.eventhandlers.LeagueNotificationEventTranslator;
import com.villo.truco.application.eventhandlers.MatchEventMapper;
import com.villo.truco.application.eventhandlers.MatchNotificationEventTranslator;
import com.villo.truco.application.eventhandlers.MatchRecipientResolver;
import com.villo.truco.application.ports.out.ApplicationEventHandler;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import com.villo.truco.infrastructure.actuator.metrics.CupEventMetricsEventHandler;
import com.villo.truco.infrastructure.actuator.metrics.LeagueEventMetricsEventHandler;
import com.villo.truco.infrastructure.actuator.metrics.MatchEventMetricsEventHandler;
import com.villo.truco.infrastructure.events.InProcessApplicationEventPublisher;
import com.villo.truco.infrastructure.events.TransactionalApplicationEventPublisher;
import com.villo.truco.infrastructure.websocket.StompCupNotificationHandler;
import com.villo.truco.infrastructure.websocket.StompLeagueNotificationHandler;
import com.villo.truco.infrastructure.websocket.StompMatchNotificationHandler;
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
  MatchEventMapper matchEventMapper() {

    return new MatchEventMapper();
  }

  @Bean
  MatchRecipientResolver matchRecipientResolver() {

    return new MatchRecipientResolver();
  }

  @Bean
  CupEventMapper cupEventMapper() {

    return new CupEventMapper();
  }

  @Bean
  LeagueEventMapper leagueEventMapper() {

    return new LeagueEventMapper();
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
      final ApplicationEventPublisher publisher) {

    return new CupNotificationEventTranslator(cupEventMapper(), publisher);
  }

  @Bean
  LeagueNotificationEventTranslator leagueNotificationTranslator(
      final ApplicationEventPublisher publisher) {

    return new LeagueNotificationEventTranslator(leagueEventMapper(), publisher);
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
