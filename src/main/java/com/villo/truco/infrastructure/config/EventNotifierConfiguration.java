package com.villo.truco.infrastructure.config;

import com.villo.truco.application.eventhandlers.CupMatchFinishedEventHandler;
import com.villo.truco.application.eventhandlers.CupMatchForfeitedEventHandler;
import com.villo.truco.application.eventhandlers.LeagueMatchFinishedEventHandler;
import com.villo.truco.application.eventhandlers.LeagueMatchForfeitedEventHandler;
import com.villo.truco.application.ports.in.AdvanceCupUseCase;
import com.villo.truco.application.ports.in.AdvanceLeagueUseCase;
import com.villo.truco.application.ports.in.ForfeitCupUseCase;
import com.villo.truco.application.ports.in.ForfeitLeagueUseCase;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import com.villo.truco.infrastructure.actuator.metrics.MatchDomainEventMetricsHandler;
import com.villo.truco.infrastructure.events.CompositeMatchEventNotifier;
import com.villo.truco.infrastructure.websocket.StompMatchEventNotifier;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Configuration
public class EventNotifierConfiguration {

  private final SimpMessagingTemplate messagingTemplate;
  private final LeagueQueryRepository leagueQueryRepository;
  private final AdvanceLeagueUseCase advanceLeagueUseCase;
  private final ForfeitLeagueUseCase forfeitLeagueUseCase;
  private final CupQueryRepository cupQueryRepository;
  private final AdvanceCupUseCase advanceCupUseCase;
  private final ForfeitCupUseCase forfeitCupUseCase;
  private final EventNotifierHealthRegistry eventNotifierHealthRegistry;
  private final MeterRegistry meterRegistry;

  public EventNotifierConfiguration(final SimpMessagingTemplate messagingTemplate,
      final LeagueQueryRepository leagueQueryRepository,
      final AdvanceLeagueUseCase advanceLeagueUseCase,
      final ForfeitLeagueUseCase forfeitLeagueUseCase,
      final CupQueryRepository cupQueryRepository, final AdvanceCupUseCase advanceCupUseCase,
      final ForfeitCupUseCase forfeitCupUseCase,
      final EventNotifierHealthRegistry eventNotifierHealthRegistry,
      final MeterRegistry meterRegistry) {

    this.messagingTemplate = messagingTemplate;
    this.leagueQueryRepository = leagueQueryRepository;
    this.advanceLeagueUseCase = advanceLeagueUseCase;
    this.forfeitLeagueUseCase = forfeitLeagueUseCase;
    this.cupQueryRepository = cupQueryRepository;
    this.advanceCupUseCase = advanceCupUseCase;
    this.forfeitCupUseCase = forfeitCupUseCase;
    this.eventNotifierHealthRegistry = eventNotifierHealthRegistry;
    this.meterRegistry = meterRegistry;
  }

  @Bean
  StompMatchEventNotifier stompMatchEventNotifier() {

    return new StompMatchEventNotifier(this.messagingTemplate, this.eventNotifierHealthRegistry);
  }

  @Bean
  MatchDomainEventMetricsHandler matchDomainEventMetricsHandler() {

    return new MatchDomainEventMetricsHandler(this.meterRegistry);
  }

  @Bean
  LeagueMatchFinishedEventHandler leagueMatchFinishedHandler() {

    return new LeagueMatchFinishedEventHandler(this.leagueQueryRepository,
        this.advanceLeagueUseCase);
  }

  @Bean
  LeagueMatchForfeitedEventHandler leagueMatchForfeitedHandler() {

    return new LeagueMatchForfeitedEventHandler(this.leagueQueryRepository,
        this.forfeitLeagueUseCase);
  }

  @Bean
  CupMatchFinishedEventHandler cupMatchFinishedHandler() {

    return new CupMatchFinishedEventHandler(this.cupQueryRepository, this.advanceCupUseCase);
  }

  @Bean
  CupMatchForfeitedEventHandler cupMatchForfeitedHandler() {

    return new CupMatchForfeitedEventHandler(this.cupQueryRepository, this.forfeitCupUseCase);
  }

  @Bean
  MatchEventNotifier matchEventNotifier() {

    return new CompositeMatchEventNotifier(
        List.of(this.stompMatchEventNotifier(), this.leagueMatchFinishedHandler(),
            this.leagueMatchForfeitedHandler(), this.cupMatchFinishedHandler(),
        this.cupMatchForfeitedHandler(), this.matchDomainEventMetricsHandler()));
  }

}
