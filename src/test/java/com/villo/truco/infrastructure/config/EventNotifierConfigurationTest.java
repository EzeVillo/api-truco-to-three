package com.villo.truco.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.villo.truco.application.eventhandlers.BotDomainEventTranslator;
import com.villo.truco.application.eventhandlers.CompetitionDomainEventTranslator;
import com.villo.truco.application.eventhandlers.CupNotificationEventTranslator;
import com.villo.truco.application.eventhandlers.LeagueNotificationEventTranslator;
import com.villo.truco.application.eventhandlers.MatchNotificationEventTranslator;
import org.junit.jupiter.api.Test;

class EventNotifierConfigurationTest {

  @Test
  void buildsNotifierBeans() {

    final var configuration = new EventNotifierConfiguration(
        mock(MatchNotificationEventTranslator.class), mock(CompetitionDomainEventTranslator.class),
        mock(BotDomainEventTranslator.class), mock(CupNotificationEventTranslator.class),
        mock(LeagueNotificationEventTranslator.class));

    assertThat(configuration.matchEventNotifier()).isNotNull();
    assertThat(configuration.cupEventNotifier()).isNotNull();
    assertThat(configuration.leagueEventNotifier()).isNotNull();
  }

}
