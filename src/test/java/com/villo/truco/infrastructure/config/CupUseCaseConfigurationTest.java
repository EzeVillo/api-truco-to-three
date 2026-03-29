package com.villo.truco.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.domain.ports.CupEventNotifier;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.infrastructure.pipeline.UseCasePipeline;
import java.util.List;
import org.junit.jupiter.api.Test;

class CupUseCaseConfigurationTest {

  @Test
  void buildsCupBeans() {

    final var availabilityConfiguration = new PlayerAvailabilityConfiguration(
        mock(MatchQueryRepository.class), mock(LeagueQueryRepository.class),
        mock(CupQueryRepository.class), mock(BotRegistry.class));

    final var configuration = new CupUseCaseConfiguration(mock(CupQueryRepository.class),
        mock(com.villo.truco.domain.ports.CupRepository.class), mock(MatchRepository.class),
        mock(CupEventNotifier.class), availabilityConfiguration.playerAvailabilityChecker(),
        new UseCasePipeline(List.of()), new UseCasePipeline(List.of()));

    assertThat(configuration.cupResolver()).isNotNull();
    assertThat(configuration.getCupStateQueryHandler()).isNotNull();
  }

}
