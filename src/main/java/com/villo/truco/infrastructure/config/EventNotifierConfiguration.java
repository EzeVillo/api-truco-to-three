package com.villo.truco.infrastructure.config;

import com.villo.truco.application.eventhandlers.BotDomainEventTranslator;
import com.villo.truco.application.eventhandlers.ChatCupCancelledEventHandler;
import com.villo.truco.application.eventhandlers.ChatCupFinishedEventHandler;
import com.villo.truco.application.eventhandlers.ChatCupStartedEventHandler;
import com.villo.truco.application.eventhandlers.ChatLeagueCancelledEventHandler;
import com.villo.truco.application.eventhandlers.ChatLeagueFinishedEventHandler;
import com.villo.truco.application.eventhandlers.ChatLeagueStartedEventHandler;
import com.villo.truco.application.eventhandlers.ChatMatchFinishedEventHandler;
import com.villo.truco.application.eventhandlers.ChatMatchForfeitedEventHandler;
import com.villo.truco.application.eventhandlers.ChatMatchGameStartedEventHandler;
import com.villo.truco.application.eventhandlers.ChatNotificationEventTranslator;
import com.villo.truco.application.eventhandlers.CompetitionDomainEventTranslator;
import com.villo.truco.application.eventhandlers.CupNotificationEventTranslator;
import com.villo.truco.application.eventhandlers.LeagueNotificationEventTranslator;
import com.villo.truco.application.eventhandlers.MatchNotificationEventTranslator;
import com.villo.truco.application.ports.out.CupDomainEventHandler;
import com.villo.truco.application.ports.out.LeagueDomainEventHandler;
import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.domain.ports.ChatEventNotifier;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.ports.ChatRepository;
import com.villo.truco.domain.ports.CupEventNotifier;
import com.villo.truco.domain.ports.LeagueEventNotifier;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.infrastructure.events.CompositeChatEventNotifier;
import com.villo.truco.infrastructure.events.CompositeCupEventNotifier;
import com.villo.truco.infrastructure.events.CompositeLeagueEventNotifier;
import com.villo.truco.infrastructure.events.MatchDomainEventDispatcher;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class EventNotifierConfiguration {

  private final ChatNotificationEventTranslator chatNotificationEventTranslator;
  private final MatchNotificationEventTranslator matchNotificationEventTranslator;
  private final CompetitionDomainEventTranslator competitionDomainEventTranslator;
  private final BotDomainEventTranslator botDomainEventTranslator;
  private final CupNotificationEventTranslator cupNotificationEventTranslator;
  private final LeagueNotificationEventTranslator leagueNotificationEventTranslator;
  private final ChatRepository chatRepository;
  private final ChatQueryRepository chatQueryRepository;

  public EventNotifierConfiguration(
      final ChatNotificationEventTranslator chatNotificationEventTranslator,
      final MatchNotificationEventTranslator matchNotificationEventTranslator,
      final CompetitionDomainEventTranslator competitionDomainEventTranslator,
      final BotDomainEventTranslator botDomainEventTranslator,
      final CupNotificationEventTranslator cupNotificationEventTranslator,
      final LeagueNotificationEventTranslator leagueNotificationEventTranslator,
      final ChatRepository chatRepository, final ChatQueryRepository chatQueryRepository) {

    this.chatNotificationEventTranslator = chatNotificationEventTranslator;
    this.matchNotificationEventTranslator = matchNotificationEventTranslator;
    this.competitionDomainEventTranslator = competitionDomainEventTranslator;
    this.botDomainEventTranslator = botDomainEventTranslator;
    this.cupNotificationEventTranslator = cupNotificationEventTranslator;
    this.leagueNotificationEventTranslator = leagueNotificationEventTranslator;
    this.chatRepository = chatRepository;
    this.chatQueryRepository = chatQueryRepository;
  }

  @Bean
  ChatLeagueStartedEventHandler chatLeagueStartedHandler(
      @Lazy final ChatEventNotifier chatEventNotifier) {

    return new ChatLeagueStartedEventHandler(this.chatRepository, this.chatQueryRepository,
        chatEventNotifier);
  }

  @Bean
  ChatLeagueFinishedEventHandler chatLeagueFinishedHandler() {

    return new ChatLeagueFinishedEventHandler(this.chatRepository, this.chatQueryRepository);
  }

  @Bean
  ChatLeagueCancelledEventHandler chatLeagueCancelledHandler() {

    return new ChatLeagueCancelledEventHandler(this.chatRepository, this.chatQueryRepository);
  }

  @Bean
  ChatCupStartedEventHandler chatCupStartedHandler(
      @Lazy final ChatEventNotifier chatEventNotifier) {

    return new ChatCupStartedEventHandler(this.chatRepository, this.chatQueryRepository,
        chatEventNotifier);
  }

  @Bean
  ChatCupFinishedEventHandler chatCupFinishedHandler() {

    return new ChatCupFinishedEventHandler(this.chatRepository, this.chatQueryRepository);
  }

  @Bean
  ChatCupCancelledEventHandler chatCupCancelledHandler() {

    return new ChatCupCancelledEventHandler(this.chatRepository, this.chatQueryRepository);
  }

  @Bean
  ChatMatchGameStartedEventHandler chatMatchGameStartedHandler(
      @Lazy final ChatEventNotifier chatEventNotifier) {

    return new ChatMatchGameStartedEventHandler(this.chatRepository, this.chatQueryRepository,
        chatEventNotifier);
  }

  @Bean
  ChatMatchFinishedEventHandler chatMatchFinishedHandler() {

    return new ChatMatchFinishedEventHandler(this.chatRepository, this.chatQueryRepository);
  }

  @Bean
  ChatMatchForfeitedEventHandler chatMatchForfeitedHandler() {

    return new ChatMatchForfeitedEventHandler(this.chatRepository, this.chatQueryRepository);
  }

  @Bean
  ChatEventNotifier chatEventNotifier() {

    return new CompositeChatEventNotifier(List.of(this.chatNotificationEventTranslator));
  }

  @Bean
  MatchEventNotifier matchEventNotifier() {

    final List<MatchDomainEventHandler<?>> handlers = List.of(this.matchNotificationEventTranslator,
        this.competitionDomainEventTranslator, this.botDomainEventTranslator,
        this.chatMatchGameStartedHandler(chatEventNotifier()), this.chatMatchFinishedHandler(),
        this.chatMatchForfeitedHandler());
    return new MatchDomainEventDispatcher(handlers);
  }

  @Bean
  CupEventNotifier cupEventNotifier() {

    final List<CupDomainEventHandler<?>> handlers = List.of(this.cupNotificationEventTranslator,
        this.chatCupStartedHandler(chatEventNotifier()), this.chatCupFinishedHandler(),
        this.chatCupCancelledHandler());
    return new CompositeCupEventNotifier(handlers);
  }

  @Bean
  LeagueEventNotifier leagueEventNotifier() {

    final List<LeagueDomainEventHandler<?>> handlers = List.of(
        this.leagueNotificationEventTranslator, this.chatLeagueStartedHandler(chatEventNotifier()),
        this.chatLeagueFinishedHandler(), this.chatLeagueCancelledHandler());
    return new CompositeLeagueEventNotifier(handlers);
  }

}
