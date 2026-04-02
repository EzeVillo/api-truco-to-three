package com.villo.truco.auth.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.auth.application.commands.RegisterUserCommand;
import com.villo.truco.auth.application.services.UserSessionIssuer;
import com.villo.truco.auth.domain.model.user.User;
import com.villo.truco.auth.domain.model.user.UserRehydrator;
import com.villo.truco.auth.domain.model.user.UserSnapshot;
import com.villo.truco.auth.domain.model.user.UsernameAvailabilityPolicy;
import com.villo.truco.auth.domain.model.user.exceptions.InvalidPasswordException;
import com.villo.truco.auth.domain.model.user.exceptions.UsernameUnavailableException;
import com.villo.truco.auth.domain.model.user.valueobjects.HashedPassword;
import com.villo.truco.auth.domain.model.user.valueobjects.RawPassword;
import com.villo.truco.auth.domain.model.user.valueobjects.Username;
import com.villo.truco.auth.domain.ports.PasswordHasher;
import com.villo.truco.auth.domain.ports.UserRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RegisterUserCommandHandler")
class RegisterUserCommandHandlerTest {

  private static PasswordHasher stubHasher() {

    return new PasswordHasher() {

      @Override
      public HashedPassword hash(final RawPassword rawPassword) {

        return new HashedPassword("hashed:" + rawPassword.value());
      }

      @Override
      public boolean matches(final String rawPassword, final HashedPassword hashedPassword) {

        return hashedPassword.value().equals("hashed:" + rawPassword);
      }
    };
  }

  @Test
  @DisplayName("registra un usuario nuevo y devuelve tokens")
  void registersNewUser() {

    final var savedUser = new AtomicReference<User>();
    final var userSessions = new AuthTestFixtures.InMemoryUserSessionRepository();
    final UserRepository repository = new UserRepository() {

      @Override
      public void saveEnsuringUsernameAvailable(final User user) {

        savedUser.set(user);
      }

      @Override
      public Optional<User> findByUsername(final Username username) {

        return Optional.ofNullable(savedUser.get());
      }

      @Override
      public boolean existsByUsername(final Username username) {

        return savedUser.get() != null && savedUser.get().username().value()
            .equals(username.value());
      }
    };

    final var issuer = new UserSessionIssuer(AuthTestFixtures.stubAccessTokenIssuer(),
        AuthTestFixtures.constantRefreshTokenProvider("refresh-value"), userSessions,
        AuthTestFixtures.fixedClock());
    final var handler = new RegisterUserCommandHandler(repository, stubHasher(), issuer,
        new UsernameAvailabilityPolicy(repository));

    final var session = handler.handle(new RegisterUserCommand("juancho", "pass1!"));

    assertThat(session.playerId()).isNotNull();
    assertThat(session.accessToken()).startsWith("access-");
    assertThat(session.refreshToken()).isEqualTo("refresh-value");
    assertThat(session.accessTokenExpiresIn()).isEqualTo(
        AuthTestFixtures.USER_ACCESS_TOKEN_EXPIRES_IN);
    assertThat(session.refreshTokenExpiresIn()).isEqualTo(
        AuthTestFixtures.REFRESH_TOKEN_EXPIRES_IN);
    assertThat(savedUser.get()).isNotNull();
    assertThat(savedUser.get().username().value()).isEqualTo("juancho");
    assertThat(savedUser.get().hashedPassword().value()).isEqualTo("hashed:pass1!");
    assertThat(userSessions.findByRefreshTokenHash("hash-refresh-value")).isPresent();
  }

  @Test
  @DisplayName("falla si el username ya existe")
  void failsIfUsernameTaken() {

    final Map<String, User> store = new HashMap<>();
    store.put("juancho", UserRehydrator.rehydrate(
        new UserSnapshot(PlayerId.generate(), new Username("juancho"),
            new HashedPassword("hashed"))));

    final UserRepository repository = new UserRepository() {

      @Override
      public void saveEnsuringUsernameAvailable(final User user) {

        store.put(user.username().value(), user);
      }

      @Override
      public Optional<User> findByUsername(final Username username) {

        return Optional.ofNullable(store.get(username.value()));
      }

      @Override
      public boolean existsByUsername(final Username username) {

        return store.containsKey(username.value());
      }
    };

    final var issuer = new UserSessionIssuer(AuthTestFixtures.stubAccessTokenIssuer(),
        AuthTestFixtures.constantRefreshTokenProvider("refresh-value"),
        new AuthTestFixtures.InMemoryUserSessionRepository(), AuthTestFixtures.fixedClock());
    final var handler = new RegisterUserCommandHandler(repository, stubHasher(), issuer,
        new UsernameAvailabilityPolicy(repository));

    assertThatThrownBy(
        () -> handler.handle(new RegisterUserCommand("juancho", "other1!"))).isInstanceOf(
        UsernameUnavailableException.class);
  }

  @Test
  @DisplayName("falla si la password no cumple la politica de registro")
  void failsIfPasswordDoesNotMeetPolicy() {

    final UserRepository repository = new UserRepository() {

      @Override
      public void saveEnsuringUsernameAvailable(final User user) {

      }

      @Override
      public Optional<User> findByUsername(final Username username) {

        return Optional.empty();
      }

      @Override
      public boolean existsByUsername(final Username username) {

        return false;
      }
    };

    final var issuer = new UserSessionIssuer(AuthTestFixtures.stubAccessTokenIssuer(),
        AuthTestFixtures.constantRefreshTokenProvider("refresh-value"),
        new AuthTestFixtures.InMemoryUserSessionRepository(), AuthTestFixtures.fixedClock());
    final var handler = new RegisterUserCommandHandler(repository, stubHasher(), issuer,
        new UsernameAvailabilityPolicy(repository));

    assertThatThrownBy(
        () -> handler.handle(new RegisterUserCommand("juancho", "abcde"))).isInstanceOf(
        InvalidPasswordException.class).hasMessage("Password must contain at least 1 number");
  }

}
