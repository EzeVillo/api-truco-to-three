package com.villo.truco.infrastructure.config;

import com.villo.truco.application.eventhandlers.BotDomainEventTranslator;
import com.villo.truco.application.eventhandlers.CompetitionDomainEventTranslator;
import com.villo.truco.application.eventhandlers.CupNotificationEventTranslator;
import com.villo.truco.application.eventhandlers.LeagueNotificationEventTranslator;
import com.villo.truco.application.eventhandlers.MatchNotificationEventTranslator;
import com.villo.truco.application.ports.out.CupDomainEventHandler;
import com.villo.truco.application.ports.out.LeagueDomainEventHandler;
import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.domain.ports.CupEventNotifier;
import com.villo.truco.domain.ports.LeagueEventNotifier;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.infrastructure.events.CompositeCupEventNotifier;
import com.villo.truco.infrastructure.events.CompositeLeagueEventNotifier;
import com.villo.truco.infrastructure.events.MatchDomainEventDispatcher;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventNotifierConfiguration {

  private final MatchNotificationEventTranslator matchNotificationEventTranslator;
  private final CompetitionDomainEventTranslator competitionDomainEventTranslator;
  private final BotDomainEventTranslator botDomainEventTranslator;
  private final CupNotificationEventTranslator cupNotificationEventTranslator;
  private final LeagueNotificationEventTranslator leagueNotificationEventTranslator;

  public EventNotifierConfiguration(final MatchNotificationEventTranslator matchNotificationEventTranslator,
      final CompetitionDomainEventTranslator competitionDomainEventTranslator,
      final BotDomainEventTranslator botDomainEventTranslator,
      final CupNotificationEventTranslator cupNotificationEventTranslator,
      final LeagueNotificationEventTranslator leagueNotificationEventTranslator) {

    this.matchNotificationEventTranslator = matchNotificationEventTranslator;
    this.competitionDomainEventTranslator = competitionDomainEventTranslator;
    this.botDomainEventTranslator = botDomainEventTranslator;
    this.cupNotificationEventTranslator = cupNotificationEventTranslator;
    this.leagueNotificationEventTranslator = leagueNotificationEventTranslator;
  }

  @Bean
  MatchEventNotifier matchEventNotifier() {

    final List<MatchDomainEventHandler<?>> handlers = List.of(this.matchNotificationEventTranslator,
        this.competitionDomainEventTranslator, this.botDomainEventTranslator);
    return new MatchDomainEventDispatcher(handlers);
  }

  @Bean
  CupEventNotifier cupEventNotifier() {

    final List<CupDomainEventHandler<?>> handlers = List.of(this.cupNotificationEventTranslator);
    return new CompositeCupEventNotifier(handlers);
  }

  @Bean
  LeagueEventNotifier leagueEventNotifier() {

    final List<LeagueDomainEventHandler<?>> handlers = List.of(this.leagueNotificationEventTranslator);
    return new CompositeLeagueEventNotifier(handlers);
  }

}
