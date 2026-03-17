package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.application.commands.RegisterUserCommand;
import com.villo.truco.application.exceptions.UsernameAlreadyTakenException;
import com.villo.truco.application.ports.PasswordHasher;
import com.villo.truco.application.ports.PlayerTokenProvider;
import com.villo.truco.domain.model.user.User;
import com.villo.truco.domain.ports.UserRepository;
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
      public String hash(final String rawPassword) {

        return "hashed:" + rawPassword;
      }

      @Override
      public boolean matches(final String rawPassword, final String hashedPassword) {

        return hashedPassword.equals("hashed:" + rawPassword);
      }
    };
  }

  private static PlayerTokenProvider stubTokenProvider() {

    return playerId -> "token-" + playerId.value();
  }

  @Test
  @DisplayName("registra un usuario nuevo y devuelve playerId y token")
  void registersNewUser() {

    final var savedUser = new AtomicReference<User>();
    final UserRepository repository = new UserRepository() {

      @Override
      public void save(final User user) {

        savedUser.set(user);
      }

      @Override
      public Optional<User> findByUsername(final String username) {

        return Optional.ofNullable(savedUser.get());
      }

      @Override
      public boolean existsByUsername(final String username) {

        return savedUser.get() != null && savedUser.get().username().equals(username);
      }
    };

    final var handler = new RegisterUserCommandHandler(repository, stubHasher(),
        stubTokenProvider());

    final var dto = handler.handle(new RegisterUserCommand("juancho", "pass123"));

    assertThat(dto.playerId()).isNotNull();
    assertThat(dto.accessToken()).isNotNull();
    assertThat(savedUser.get()).isNotNull();
    assertThat(savedUser.get().username()).isEqualTo("juancho");
  }

  @Test
  @DisplayName("falla si el username ya existe")
  void failsIfUsernameTaken() {

    final Map<String, User> store = new HashMap<>();
    store.put("juancho", new User(PlayerId.generate(), "juancho", "hashed"));

    final UserRepository repository = new UserRepository() {

      @Override
      public void save(final User user) {

        store.put(user.username(), user);
      }

      @Override
      public Optional<User> findByUsername(final String username) {

        return Optional.ofNullable(store.get(username));
      }

      @Override
      public boolean existsByUsername(final String username) {

        return store.containsKey(username);
      }
    };

    final var handler = new RegisterUserCommandHandler(repository, stubHasher(),
        stubTokenProvider());

    assertThatThrownBy(
        () -> handler.handle(new RegisterUserCommand("juancho", "otherpass"))).isInstanceOf(
        UsernameAlreadyTakenException.class);
  }

}
