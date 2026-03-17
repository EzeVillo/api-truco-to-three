package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.LoginCommand;
import com.villo.truco.application.dto.LoginDTO;
import com.villo.truco.application.exceptions.InvalidCredentialsException;
import com.villo.truco.application.ports.PasswordHasher;
import com.villo.truco.application.ports.PlayerTokenProvider;
import com.villo.truco.application.ports.in.LoginUseCase;
import com.villo.truco.domain.ports.UserRepository;
import java.util.Objects;

public final class LoginCommandHandler implements LoginUseCase {

  private final UserRepository userRepository;
  private final PasswordHasher passwordHasher;
  private final PlayerTokenProvider tokenProvider;

  public LoginCommandHandler(final UserRepository userRepository,
      final PasswordHasher passwordHasher, final PlayerTokenProvider tokenProvider) {

    this.userRepository = Objects.requireNonNull(userRepository);
    this.passwordHasher = Objects.requireNonNull(passwordHasher);
    this.tokenProvider = Objects.requireNonNull(tokenProvider);
  }

  @Override
  public LoginDTO handle(final LoginCommand command) {

    final var user = this.userRepository.findByUsername(command.username())
        .orElseThrow(InvalidCredentialsException::new);

    if (!this.passwordHasher.matches(command.password(), user.hashedPassword())) {
      throw new InvalidCredentialsException();
    }

    final var accessToken = this.tokenProvider.generateAccessToken(user.id());

    return new LoginDTO(user.id().value().toString(), accessToken);
  }

}
