package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.application.commands.LoginCommand;
import com.villo.truco.application.exceptions.InvalidCredentialsException;
import com.villo.truco.application.ports.PasswordHasher;
import com.villo.truco.application.ports.PlayerTokenProvider;
import com.villo.truco.domain.model.user.User;
import com.villo.truco.domain.ports.UserRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LoginCommandHandler")
class LoginCommandHandlerTest {

  private static final PlayerId PLAYER_ID = PlayerId.generate();
  private static final String USERNAME = "juancho";
  private static final String RAW_PASSWORD = "secret";
  private static final String HASHED_PASSWORD = "hashed:secret";

  private final PasswordHasher passwordHasher = new PasswordHasher() {

    @Override
    public String hash(final String rawPassword) {

      return "hashed:" + rawPassword;
    }

    @Override
    public boolean matches(final String rawPassword, final String hashedPassword) {

      return hashedPassword.equals("hashed:" + rawPassword);
    }
  };

  private final PlayerTokenProvider tokenProvider = playerId -> "token-" + playerId.value();

  private UserRepository userRepository;

  @BeforeEach
  void setUp() {

    final var user = new User(PLAYER_ID, USERNAME, HASHED_PASSWORD);
    this.userRepository = new UserRepository() {

      @Override
      public void save(final User u) {

      }

      @Override
      public Optional<User> findByUsername(final String username) {

        return USERNAME.equals(username) ? Optional.of(user) : Optional.empty();
      }

      @Override
      public boolean existsByUsername(final String username) {

        return USERNAME.equals(username);
      }
    };
  }

  @Test
  @DisplayName("login exitoso devuelve playerId y token")
  void loginSucceeds() {

    final var handler = new LoginCommandHandler(this.userRepository, this.passwordHasher,
        this.tokenProvider);

    final var dto = handler.handle(new LoginCommand(USERNAME, RAW_PASSWORD));

    assertThat(dto.playerId()).isEqualTo(PLAYER_ID.value().toString());
    assertThat(dto.accessToken()).isNotNull();
  }

  @Test
  @DisplayName("falla si el username no existe")
  void failsIfUsernameNotFound() {

    final var handler = new LoginCommandHandler(this.userRepository, this.passwordHasher,
        this.tokenProvider);

    assertThatThrownBy(
        () -> handler.handle(new LoginCommand("unknown", RAW_PASSWORD))).isInstanceOf(
        InvalidCredentialsException.class);
  }

  @Test
  @DisplayName("falla si la contraseña es incorrecta")
  void failsIfWrongPassword() {

    final var handler = new LoginCommandHandler(this.userRepository, this.passwordHasher,
        this.tokenProvider);

    assertThatThrownBy(() -> handler.handle(new LoginCommand(USERNAME, "wrongpass"))).isInstanceOf(
        InvalidCredentialsException.class);
  }

}
