package com.villo.truco.auth.domain.model.user;

import com.villo.truco.auth.domain.model.user.valueobjects.HashedPassword;
import com.villo.truco.auth.domain.model.user.valueobjects.RawPassword;
import com.villo.truco.auth.domain.model.user.valueobjects.Username;
import com.villo.truco.auth.domain.ports.PasswordHasher;
import com.villo.truco.domain.shared.AggregateBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class User extends AggregateBase<PlayerId> {

  private final Username username;
  private final HashedPassword hashedPassword;

  private User(final PlayerId id, final Username username, final HashedPassword hashedPassword) {

    super(id);

    Objects.requireNonNull(username, "username must not be null");
    Objects.requireNonNull(hashedPassword, "hashedPassword must not be null");

    this.username = username;
    this.hashedPassword = hashedPassword;
  }

  public User(final PlayerId id, final Username username, final RawPassword rawPassword,
      final PasswordHasher passwordHasher) {

    this(id, username, passwordHasher.hash(rawPassword));
  }

  static User reconstruct(final PlayerId id, final Username username,
      final HashedPassword hashedPassword) {

    return new User(id, username, hashedPassword);
  }

  public Username username() {

    return this.username;
  }

  public HashedPassword hashedPassword() {

    return this.hashedPassword;
  }

  public boolean matchesPassword(final String rawPassword, final PasswordHasher passwordHasher) {

    return this.hashedPassword.matches(rawPassword, passwordHasher);
  }

  public UserSnapshot snapshot() {

    return new UserSnapshot(this.getId(), this.username, this.hashedPassword);
  }

}
