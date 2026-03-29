package com.villo.truco.infrastructure.config;

import com.villo.truco.application.ports.PasswordHasher;
import com.villo.truco.application.ports.PlayerTokenProvider;
import com.villo.truco.application.ports.in.GuestLoginUseCase;
import com.villo.truco.application.ports.in.LoginUseCase;
import com.villo.truco.application.ports.in.RegisterUserUseCase;
import com.villo.truco.application.usecases.commands.GuestLoginCommandHandler;
import com.villo.truco.application.usecases.commands.LoginCommandHandler;
import com.villo.truco.application.usecases.commands.RegisterUserCommandHandler;
import com.villo.truco.domain.ports.UserRepository;
import com.villo.truco.infrastructure.pipeline.UseCasePipeline;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthUseCaseConfiguration {

  private final UserRepository userRepository;
  private final PasswordHasher passwordHasher;
  private final PlayerTokenProvider tokenProvider;
  private final UseCasePipeline transactionalPipeline;

  public AuthUseCaseConfiguration(final UserRepository userRepository,
      final PasswordHasher passwordHasher, final PlayerTokenProvider tokenProvider,
      @Qualifier("transactionalPipeline") final UseCasePipeline transactionalPipeline) {

    this.userRepository = userRepository;
    this.passwordHasher = passwordHasher;
    this.tokenProvider = tokenProvider;
    this.transactionalPipeline = transactionalPipeline;
  }

  @Bean
  RegisterUserUseCase registerUserCommandHandler() {

    final var handler = new RegisterUserCommandHandler(this.userRepository, this.passwordHasher,
        this.tokenProvider);
    return this.transactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  LoginUseCase loginCommandHandler() {

    final var handler = new LoginCommandHandler(this.userRepository, this.passwordHasher,
        this.tokenProvider);
    return this.transactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  GuestLoginUseCase guestLoginCommandHandler() {

    return new GuestLoginCommandHandler(this.tokenProvider);
  }

}
