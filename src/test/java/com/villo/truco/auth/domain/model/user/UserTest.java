package com.villo.truco.auth.domain.model.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.auth.domain.model.user.exceptions.InvalidPasswordException;
import com.villo.truco.auth.domain.model.user.exceptions.InvalidUsernameException;
import com.villo.truco.auth.domain.model.user.valueobjects.HashedPassword;
import com.villo.truco.auth.domain.model.user.valueobjects.RawPassword;
import com.villo.truco.auth.domain.model.user.valueobjects.Username;
import com.villo.truco.auth.domain.ports.PasswordHasher;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("User")
class UserTest {

  @Test
  @DisplayName("rechaza usernames con menos de 3 letras")
  void rejectsUsernameWithLessThanThreeLetters() {

    assertThatThrownBy(() -> new Username("ab")).isInstanceOf(InvalidUsernameException.class)
        .hasMessage("Username must contain at least 3 letters");
  }

  @Test
  @DisplayName("rechaza usernames con caracteres no alfanumericos ascii")
  void rejectsUsernameWithNonAlphanumericAsciiCharacters() {

    assertThatThrownBy(() -> new Username("abc!")).isInstanceOf(InvalidUsernameException.class)
        .hasMessage("Username must contain only ASCII letters and numbers");
  }

  @Test
  @DisplayName("hashea la password raw al construir el usuario")
  void hashesRawPasswordWhenConstructingUser() {

    final var hasher = new PasswordHasher() {

      @Override
      public HashedPassword hash(final RawPassword rawPassword) {

        return new HashedPassword("hashed:" + rawPassword.value());
      }

      @Override
      public boolean matches(final String rawPassword, final HashedPassword hashedPassword) {

        return hashedPassword.value().equals("hashed:" + rawPassword);
      }
    };

    final var user = new User(PlayerId.generate(), new Username("juancho"),
        new RawPassword("Clave1!"), hasher);

    assertThat(user.hashedPassword().value()).isEqualTo("hashed:Clave1!");
  }

  @Test
  @DisplayName("rechaza passwords invalidas al construir el usuario")
  void rejectsInvalidPasswordWhenConstructingUser() {

    final var hasher = new PasswordHasher() {

      @Override
      public HashedPassword hash(final RawPassword rawPassword) {

        return new HashedPassword("hashed:" + rawPassword.value());
      }

      @Override
      public boolean matches(final String rawPassword, final HashedPassword hashedPassword) {

        return hashedPassword.value().equals("hashed:" + rawPassword);
      }
    };

    assertThatThrownBy(
        () -> new User(PlayerId.generate(), new Username("juancho"), new RawPassword("abcde"),
            hasher)).isInstanceOf(InvalidPasswordException.class)
        .hasMessage("Password must contain at least 1 number");
  }

  @Test
  @DisplayName("rehydrata desde snapshot sin rehasear")
  void rehydratesFromSnapshot() {

    final var snapshot = new UserSnapshot(PlayerId.generate(), new Username("juancho"),
        new HashedPassword("hashed:stored"));

    final var user = UserRehydrator.rehydrate(snapshot);

    assertThat(user.getId()).isEqualTo(snapshot.id());
    assertThat(user.username()).isEqualTo(snapshot.username());
    assertThat(user.hashedPassword()).isEqualTo(snapshot.hashedPassword());
  }

}
