package com.villo.truco.infrastructure.config;

import com.villo.truco.application.ports.PasswordHasher;
import com.villo.truco.application.ports.PlayerTokenProvider;
import com.villo.truco.application.ports.in.GuestLoginUseCase;
import com.villo.truco.application.ports.in.LoginUseCase;
import com.villo.truco.application.ports.in.RegisterUserUseCase;
import com.villo.truco.application.usecases.commands.GuestLoginCommandHandler;
import com.villo.truco.application.usecases.commands.LoginCommandHandler;
import com.villo.truco.application.usecases.commands.PlayerAvailabilityChecker;
import com.villo.truco.application.usecases.commands.RegisterUserCommandHandler;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchQueryRepository;
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
  private final MatchQueryRepository matchQueryRepository;
  private final LeagueQueryRepository leagueQueryRepository;
  private final CupQueryRepository cupQueryRepository;
  private final UseCasePipeline transactionalPipeline;

  public AuthUseCaseConfiguration(final UserRepository userRepository,
      final PasswordHasher passwordHasher, final PlayerTokenProvider tokenProvider,
      final MatchQueryRepository matchQueryRepository,
      final LeagueQueryRepository leagueQueryRepository,
      final CupQueryRepository cupQueryRepository,
      @Qualifier("transactionalPipeline") final UseCasePipeline transactionalPipeline) {

    this.userRepository = userRepository;
    this.passwordHasher = passwordHasher;
    this.tokenProvider = tokenProvider;
    this.matchQueryRepository = matchQueryRepository;
    this.leagueQueryRepository = leagueQueryRepository;
    this.cupQueryRepository = cupQueryRepository;
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

  @Bean
  PlayerAvailabilityChecker playerAvailabilityChecker() {

    return new PlayerAvailabilityChecker(this.matchQueryRepository, this.leagueQueryRepository,
        this.cupQueryRepository);
  }

}
