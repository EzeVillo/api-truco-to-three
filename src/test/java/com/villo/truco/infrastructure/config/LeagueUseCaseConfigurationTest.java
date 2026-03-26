package com.villo.truco.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.villo.truco.application.ports.PasswordHasher;
import com.villo.truco.application.ports.PlayerTokenProvider;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueEventNotifier;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.ports.UserRepository;
import com.villo.truco.infrastructure.pipeline.UseCasePipeline;
import java.util.List;
import org.junit.jupiter.api.Test;

class LeagueUseCaseConfigurationTest {

  @Test
  void buildsLeagueBeans() {

    final var authConfiguration = new AuthUseCaseConfiguration(mock(UserRepository.class),
        mock(PasswordHasher.class), mock(PlayerTokenProvider.class),
        mock(MatchQueryRepository.class), mock(LeagueQueryRepository.class),
        mock(CupQueryRepository.class), new UseCasePipeline(List.of()));

    final var configuration = new LeagueUseCaseConfiguration(mock(LeagueQueryRepository.class),
        mock(com.villo.truco.domain.ports.LeagueRepository.class), mock(MatchRepository.class),
        mock(LeagueEventNotifier.class), authConfiguration.playerAvailabilityChecker(),
        new UseCasePipeline(List.of()), new UseCasePipeline(List.of()));

    assertThat(configuration.leagueResolver()).isNotNull();
    assertThat(configuration.getLeagueStateQueryHandler()).isNotNull();
  }

}
