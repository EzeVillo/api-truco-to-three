package com.villo.truco.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.villo.truco.application.ports.PasswordHasher;
import com.villo.truco.application.ports.PlayerTokenProvider;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.UserRepository;
import com.villo.truco.infrastructure.pipeline.UseCasePipeline;
import org.junit.jupiter.api.Test;

class AuthUseCaseConfigurationTest {

  @Test
  void buildsAuthBeans() {

    final var configuration = new AuthUseCaseConfiguration(mock(UserRepository.class),
        mock(PasswordHasher.class), mock(PlayerTokenProvider.class),
        mock(MatchQueryRepository.class), mock(LeagueQueryRepository.class),
        mock(CupQueryRepository.class), new UseCasePipeline(java.util.List.of()));

    assertThat(configuration.registerUserCommandHandler()).isNotNull();
    assertThat(configuration.loginCommandHandler()).isNotNull();
    assertThat(configuration.guestLoginCommandHandler()).isNotNull();
    assertThat(configuration.playerAvailabilityChecker()).isNotNull();
  }

}
