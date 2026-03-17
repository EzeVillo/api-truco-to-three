package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.RegisterUserCommand;
import com.villo.truco.application.dto.RegisterUserDTO;
import com.villo.truco.application.exceptions.UsernameAlreadyTakenException;
import com.villo.truco.application.ports.PasswordHasher;
import com.villo.truco.application.ports.PlayerTokenProvider;
import com.villo.truco.application.ports.in.RegisterUserUseCase;
import com.villo.truco.domain.model.user.User;
import com.villo.truco.domain.ports.UserRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class RegisterUserCommandHandler implements RegisterUserUseCase {

  private final UserRepository userRepository;
  private final PasswordHasher passwordHasher;
  private final PlayerTokenProvider tokenProvider;

  public RegisterUserCommandHandler(final UserRepository userRepository,
      final PasswordHasher passwordHasher, final PlayerTokenProvider tokenProvider) {

    this.userRepository = Objects.requireNonNull(userRepository);
    this.passwordHasher = Objects.requireNonNull(passwordHasher);
    this.tokenProvider = Objects.requireNonNull(tokenProvider);
  }

  @Override
  public RegisterUserDTO handle(final RegisterUserCommand command) {

    if (this.userRepository.existsByUsername(command.username())) {
      throw new UsernameAlreadyTakenException(command.username());
    }

    final var playerId = PlayerId.generate();
    final var hashedPassword = this.passwordHasher.hash(command.password());
    final var user = new User(playerId, command.username(), hashedPassword);

    this.userRepository.save(user);

    final var accessToken = this.tokenProvider.generateAccessToken(playerId);

    return new RegisterUserDTO(playerId.value().toString(), accessToken);
  }

}
