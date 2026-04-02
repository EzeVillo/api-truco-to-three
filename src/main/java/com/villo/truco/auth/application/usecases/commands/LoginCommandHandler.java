package com.villo.truco.auth.application.usecases.commands;

import com.villo.truco.auth.application.commands.LoginCommand;
import com.villo.truco.auth.application.exceptions.InvalidCredentialsException;
import com.villo.truco.auth.application.model.UserAuthenticatedSession;
import com.villo.truco.auth.application.ports.in.LoginUseCase;
import com.villo.truco.auth.application.services.UserSessionIssuer;
import com.villo.truco.auth.domain.model.user.valueobjects.Username;
import com.villo.truco.auth.domain.ports.PasswordHasher;
import com.villo.truco.auth.domain.ports.UserRepository;
import java.util.Objects;

public final class LoginCommandHandler implements LoginUseCase {

  private final UserRepository userRepository;
  private final PasswordHasher passwordHasher;
  private final UserSessionIssuer userSessionIssuer;

  public LoginCommandHandler(final UserRepository userRepository,
      final PasswordHasher passwordHasher, final UserSessionIssuer userSessionIssuer) {

    this.userRepository = Objects.requireNonNull(userRepository);
    this.passwordHasher = Objects.requireNonNull(passwordHasher);
    this.userSessionIssuer = Objects.requireNonNull(userSessionIssuer);
  }

  @Override
  public UserAuthenticatedSession handle(final LoginCommand command) {

    final var username = new Username(command.username());
    final var user = this.userRepository.findByUsername(username)
        .orElseThrow(InvalidCredentialsException::new);

    if (!user.matchesPassword(command.password(), this.passwordHasher)) {
      throw new InvalidCredentialsException();
    }

    return this.userSessionIssuer.issueFor(user.getId());
  }

}
