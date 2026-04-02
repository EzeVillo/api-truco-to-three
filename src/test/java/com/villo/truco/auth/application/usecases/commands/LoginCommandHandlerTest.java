package com.villo.truco.auth.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.auth.application.commands.LoginCommand;
import com.villo.truco.auth.application.exceptions.InvalidCredentialsException;
import com.villo.truco.auth.application.services.UserSessionIssuer;
import com.villo.truco.auth.domain.model.user.User;
import com.villo.truco.auth.domain.model.user.UserRehydrator;
import com.villo.truco.auth.domain.model.user.UserSnapshot;
import com.villo.truco.auth.domain.model.user.valueobjects.HashedPassword;
import com.villo.truco.auth.domain.model.user.valueobjects.RawPassword;
import com.villo.truco.auth.domain.model.user.valueobjects.Username;
import com.villo.truco.auth.domain.ports.PasswordHasher;
import com.villo.truco.auth.domain.ports.UserRepository;
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
    public HashedPassword hash(final RawPassword rawPassword) {

      return new HashedPassword("hashed:" + rawPassword.value());
    }

    @Override
    public boolean matches(final String rawPassword, final HashedPassword hashedPassword) {

      return hashedPassword.value().equals("hashed:" + rawPassword);
    }
  };

  private UserRepository userRepository;
  private AuthTestFixtures.InMemoryUserSessionRepository userSessionRepository;

  @BeforeEach
  void setUp() {

    final var user = UserRehydrator.rehydrate(
        new UserSnapshot(PLAYER_ID, new Username(USERNAME), new HashedPassword(HASHED_PASSWORD)));
    this.userSessionRepository = new AuthTestFixtures.InMemoryUserSessionRepository();
    this.userRepository = new UserRepository() {

      @Override
      public void saveEnsuringUsernameAvailable(final User u) {

      }

      @Override
      public Optional<User> findByUsername(final Username username) {

        return USERNAME.equals(username.value()) ? Optional.of(user) : Optional.empty();
      }

      @Override
      public boolean existsByUsername(final Username username) {

        return USERNAME.equals(username.value());
      }
    };
  }

  @Test
  @DisplayName("login exitoso devuelve playerId, access token y refresh token")
  void loginSucceeds() {

    final var issuer = new UserSessionIssuer(AuthTestFixtures.stubAccessTokenIssuer(),
        AuthTestFixtures.constantRefreshTokenProvider("refresh-value"), this.userSessionRepository,
        AuthTestFixtures.fixedClock());
    final var handler = new LoginCommandHandler(this.userRepository, this.passwordHasher, issuer);

    final var session = handler.handle(new LoginCommand(USERNAME, RAW_PASSWORD));

    assertThat(session.playerId()).isEqualTo(PLAYER_ID);
    assertThat(session.accessToken()).isEqualTo("access-" + PLAYER_ID.value());
    assertThat(session.refreshToken()).isEqualTo("refresh-value");
    assertThat(session.accessTokenExpiresIn()).isEqualTo(
        AuthTestFixtures.USER_ACCESS_TOKEN_EXPIRES_IN);
    assertThat(session.refreshTokenExpiresIn()).isEqualTo(
        AuthTestFixtures.REFRESH_TOKEN_EXPIRES_IN);
    assertThat(this.userSessionRepository.findByRefreshTokenHash("hash-refresh-value")).isPresent();
  }

  @Test
  @DisplayName("falla si el username no existe")
  void failsIfUsernameNotFound() {

    final var issuer = new UserSessionIssuer(AuthTestFixtures.stubAccessTokenIssuer(),
        AuthTestFixtures.constantRefreshTokenProvider("refresh-value"), this.userSessionRepository,
        AuthTestFixtures.fixedClock());
    final var handler = new LoginCommandHandler(this.userRepository, this.passwordHasher, issuer);

    assertThatThrownBy(
        () -> handler.handle(new LoginCommand("unknown", RAW_PASSWORD))).isInstanceOf(
        InvalidCredentialsException.class);
  }

  @Test
  @DisplayName("falla si la contrasena es incorrecta")
  void failsIfWrongPassword() {

    final var issuer = new UserSessionIssuer(AuthTestFixtures.stubAccessTokenIssuer(),
        AuthTestFixtures.constantRefreshTokenProvider("refresh-value"), this.userSessionRepository,
        AuthTestFixtures.fixedClock());
    final var handler = new LoginCommandHandler(this.userRepository, this.passwordHasher, issuer);

    assertThatThrownBy(() -> handler.handle(new LoginCommand(USERNAME, "wrongpass"))).isInstanceOf(
        InvalidCredentialsException.class);
  }

}
