package com.villo.truco.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.villo.truco.application.ports.TransactionalRunner;
import com.villo.truco.domain.ports.CupEventNotifier;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.CupRepository;
import com.villo.truco.domain.ports.LeagueEventNotifier;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.LeagueRepository;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import org.junit.jupiter.api.Test;

class TimeoutUseCaseConfigurationTest {

  @Test
  void buildsTimeoutHandlers() {

    final var configuration = new TimeoutUseCaseConfiguration(mock(MatchQueryRepository.class),
        mock(MatchRepository.class), mock(MatchEventNotifier.class),
        mock(LeagueQueryRepository.class), mock(LeagueRepository.class),
        mock(LeagueEventNotifier.class), mock(CupQueryRepository.class), mock(CupRepository.class),
        mock(CupEventNotifier.class), mock(TransactionalRunner.class), new MatchTimeoutProperties(),
        new LeagueTimeoutProperties(), new CupTimeoutProperties());

    assertThat(configuration.timeoutIdleMatchesCommandHandler()).isNotNull();
    assertThat(configuration.timeoutIdleLeaguesCommandHandler()).isNotNull();
    assertThat(configuration.timeoutIdleCupsCommandHandler()).isNotNull();
  }

}
