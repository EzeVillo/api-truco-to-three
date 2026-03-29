package com.villo.truco.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.villo.truco.application.ports.PasswordHasher;
import com.villo.truco.application.ports.PlayerTokenProvider;
import com.villo.truco.domain.ports.UserRepository;
import com.villo.truco.infrastructure.pipeline.UseCasePipeline;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuthUseCaseConfigurationTest {

  @Test
  void buildsAuthBeans() {

    final var configuration = new AuthUseCaseConfiguration(mock(UserRepository.class),
        mock(PasswordHasher.class), mock(PlayerTokenProvider.class),
        new UseCasePipeline(List.of()));

    assertThat(configuration.registerUserCommandHandler()).isNotNull();
    assertThat(configuration.loginCommandHandler()).isNotNull();
    assertThat(configuration.guestLoginCommandHandler()).isNotNull();
  }

}
