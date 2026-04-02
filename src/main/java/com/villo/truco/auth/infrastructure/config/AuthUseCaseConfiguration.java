package com.villo.truco.auth.infrastructure.config;

import com.villo.truco.auth.application.ports.in.GuestLoginUseCase;
import com.villo.truco.auth.application.ports.in.LoginUseCase;
import com.villo.truco.auth.application.ports.in.LogoutUserSessionUseCase;
import com.villo.truco.auth.application.ports.in.RefreshUserSessionUseCase;
import com.villo.truco.auth.application.ports.in.RegisterUserUseCase;
import com.villo.truco.auth.application.ports.out.AccessTokenIssuer;
import com.villo.truco.auth.application.ports.out.RefreshTokenProvider;
import com.villo.truco.auth.application.services.UserSessionIssuer;
import com.villo.truco.auth.application.usecases.commands.GuestLoginCommandHandler;
import com.villo.truco.auth.application.usecases.commands.LoginCommandHandler;
import com.villo.truco.auth.application.usecases.commands.LogoutUserSessionCommandHandler;
import com.villo.truco.auth.application.usecases.commands.RefreshUserSessionCommandHandler;
import com.villo.truco.auth.application.usecases.commands.RegisterUserCommandHandler;
import com.villo.truco.auth.domain.model.user.UsernameAvailabilityPolicy;
import com.villo.truco.auth.domain.ports.PasswordHasher;
import com.villo.truco.auth.domain.ports.UserRepository;
import com.villo.truco.auth.domain.ports.UserSessionRepository;
import com.villo.truco.infrastructure.pipeline.UseCasePipeline;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthUseCaseConfiguration {

  private final UserRepository userRepository;
  private final UserSessionRepository userSessionRepository;
  private final PasswordHasher passwordHasher;
  private final AccessTokenIssuer accessTokenIssuer;
  private final RefreshTokenProvider refreshTokenProvider;
  private final Clock clock;
  private final UseCasePipeline transactionalPipeline;

  public AuthUseCaseConfiguration(final UserRepository userRepository,
      final UserSessionRepository userSessionRepository, final PasswordHasher passwordHasher,
      final AccessTokenIssuer accessTokenIssuer, final RefreshTokenProvider refreshTokenProvider,
      final Clock clock,
      @Qualifier("transactionalPipeline") final UseCasePipeline transactionalPipeline) {

    this.userRepository = userRepository;
    this.userSessionRepository = userSessionRepository;
    this.passwordHasher = passwordHasher;
    this.accessTokenIssuer = accessTokenIssuer;
    this.refreshTokenProvider = refreshTokenProvider;
    this.clock = clock;
    this.transactionalPipeline = transactionalPipeline;
  }

  @Bean
  UserSessionIssuer userSessionIssuer() {

    return new UserSessionIssuer(this.accessTokenIssuer, this.refreshTokenProvider,
        this.userSessionRepository, this.clock);
  }

  @Bean
  RegisterUserUseCase registerUserCommandHandler() {

    final var handler = new RegisterUserCommandHandler(this.userRepository, this.passwordHasher,
        this.userSessionIssuer(), new UsernameAvailabilityPolicy(this.userRepository));
    return this.transactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  LoginUseCase loginCommandHandler() {

    final var handler = new LoginCommandHandler(this.userRepository, this.passwordHasher,
        this.userSessionIssuer());
    return this.transactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  GuestLoginUseCase guestLoginCommandHandler() {

    return new GuestLoginCommandHandler(this.accessTokenIssuer);
  }

  @Bean
  RefreshUserSessionUseCase refreshUserSessionCommandHandler() {

    return new RefreshUserSessionCommandHandler(this.userSessionRepository,
        this.refreshTokenProvider, this.userSessionIssuer());
  }

  @Bean
  LogoutUserSessionUseCase logoutUserSessionCommandHandler() {

    final var handler = new LogoutUserSessionCommandHandler(this.userSessionRepository,
        this.refreshTokenProvider, this.clock);
    return this.transactionalPipeline.wrap(handler)::handle;
  }

}
