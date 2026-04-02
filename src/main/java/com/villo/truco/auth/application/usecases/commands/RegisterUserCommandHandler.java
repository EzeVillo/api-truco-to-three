package com.villo.truco.auth.application.usecases.commands;

import com.villo.truco.auth.application.commands.RegisterUserCommand;
import com.villo.truco.auth.application.model.UserAuthenticatedSession;
import com.villo.truco.auth.application.ports.in.RegisterUserUseCase;
import com.villo.truco.auth.application.services.UserSessionIssuer;
import com.villo.truco.auth.domain.model.user.User;
import com.villo.truco.auth.domain.model.user.UsernameAvailabilityPolicy;
import com.villo.truco.auth.domain.model.user.valueobjects.RawPassword;
import com.villo.truco.auth.domain.model.user.valueobjects.Username;
import com.villo.truco.auth.domain.ports.PasswordHasher;
import com.villo.truco.auth.domain.ports.UserRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class RegisterUserCommandHandler implements RegisterUserUseCase {

  private final UserRepository userRepository;
  private final PasswordHasher passwordHasher;
  private final UserSessionIssuer userSessionIssuer;
  private final UsernameAvailabilityPolicy usernameAvailabilityPolicy;

  public RegisterUserCommandHandler(final UserRepository userRepository,
      final PasswordHasher passwordHasher, final UserSessionIssuer userSessionIssuer,
      final UsernameAvailabilityPolicy usernameAvailabilityPolicy) {

    this.userRepository = Objects.requireNonNull(userRepository);
    this.passwordHasher = Objects.requireNonNull(passwordHasher);
    this.userSessionIssuer = Objects.requireNonNull(userSessionIssuer);
    this.usernameAvailabilityPolicy = Objects.requireNonNull(usernameAvailabilityPolicy);
  }

  @Override
  public UserAuthenticatedSession handle(final RegisterUserCommand command) {

    final var username = new Username(command.username());
    this.usernameAvailabilityPolicy.ensureAvailable(username);

    final var playerId = PlayerId.generate();
    final var user = new User(playerId, username, new RawPassword(command.password()),
        this.passwordHasher);

    this.userRepository.saveEnsuringUsernameAvailable(user);

    return this.userSessionIssuer.issueFor(playerId);
  }

}
