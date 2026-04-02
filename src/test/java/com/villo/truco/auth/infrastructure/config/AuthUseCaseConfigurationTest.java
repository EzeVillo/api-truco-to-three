package com.villo.truco.auth.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.villo.truco.auth.application.ports.out.AccessTokenIssuer;
import com.villo.truco.auth.application.ports.out.RefreshTokenProvider;
import com.villo.truco.auth.domain.ports.PasswordHasher;
import com.villo.truco.auth.domain.ports.UserRepository;
import com.villo.truco.auth.domain.ports.UserSessionRepository;
import com.villo.truco.infrastructure.pipeline.UseCasePipeline;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuthUseCaseConfigurationTest {

  @Test
  void buildsAuthBeans() {

    final var configuration = new AuthUseCaseConfiguration(mock(UserRepository.class),
        mock(UserSessionRepository.class), mock(PasswordHasher.class),
        mock(AccessTokenIssuer.class), mock(RefreshTokenProvider.class), mock(Clock.class),
        new UseCasePipeline(List.of()));

    assertThat(configuration.userSessionIssuer()).isNotNull();
    assertThat(configuration.registerUserCommandHandler()).isNotNull();
    assertThat(configuration.loginCommandHandler()).isNotNull();
    assertThat(configuration.guestLoginCommandHandler()).isNotNull();
    assertThat(configuration.refreshUserSessionCommandHandler()).isNotNull();
    assertThat(configuration.logoutUserSessionCommandHandler()).isNotNull();
  }

}
