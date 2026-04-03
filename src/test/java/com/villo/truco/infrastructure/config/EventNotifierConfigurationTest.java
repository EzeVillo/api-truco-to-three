package com.villo.truco.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.villo.truco.application.eventhandlers.BotDomainEventTranslator;
import com.villo.truco.application.eventhandlers.ChatNotificationEventTranslator;
import com.villo.truco.application.eventhandlers.CompetitionDomainEventTranslator;
import com.villo.truco.application.eventhandlers.CupNotificationEventTranslator;
import com.villo.truco.application.eventhandlers.LeagueNotificationEventTranslator;
import com.villo.truco.application.eventhandlers.MatchNotificationEventTranslator;
import com.villo.truco.application.eventhandlers.SpectatorAutoKickOnCupMatchActivatedEventHandler;
import com.villo.truco.application.eventhandlers.SpectatorAutoKickOnLeagueMatchActivatedEventHandler;
import com.villo.truco.application.eventhandlers.SpectatorCleanupOnMatchEndEventHandler;
import com.villo.truco.application.eventhandlers.SpectatorNotificationEventTranslator;
import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.domain.ports.ChatEventNotifier;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.ports.ChatRepository;
import org.junit.jupiter.api.Test;

class EventNotifierConfigurationTest {

  @Test
  void buildsNotifierBeans() {

    final var configuration = new EventNotifierConfiguration(
        mock(ChatNotificationEventTranslator.class), mock(MatchNotificationEventTranslator.class),
        mock(CompetitionDomainEventTranslator.class), mock(BotDomainEventTranslator.class),
        mock(BotRegistry.class), mock(CupNotificationEventTranslator.class),
        mock(LeagueNotificationEventTranslator.class), mock(ChatRepository.class),
        mock(ChatQueryRepository.class), mock(SpectatorNotificationEventTranslator.class),
        mock(SpectatorCleanupOnMatchEndEventHandler.class),
        mock(SpectatorAutoKickOnLeagueMatchActivatedEventHandler.class),
        mock(SpectatorAutoKickOnCupMatchActivatedEventHandler.class));
    final var chatEventNotifier = mock(ChatEventNotifier.class);

    assertThat(configuration.chatEventNotifier()).isNotNull();
    assertThat(configuration.chatCupStartedHandler(chatEventNotifier)).isNotNull();
    assertThat(configuration.chatLeagueStartedHandler(chatEventNotifier)).isNotNull();
    assertThat(configuration.chatMatchGameStartedHandler(chatEventNotifier)).isNotNull();
    assertThat(configuration.matchEventNotifier()).isNotNull();
    assertThat(configuration.cupEventNotifier()).isNotNull();
    assertThat(configuration.leagueEventNotifier()).isNotNull();
  }

}
