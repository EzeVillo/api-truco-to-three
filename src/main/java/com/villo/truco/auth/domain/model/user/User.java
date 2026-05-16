package com.villo.truco.auth.domain.model.user;

import com.villo.truco.auth.domain.model.user.events.AuthDomainEvent;
import com.villo.truco.auth.domain.model.user.events.UserRegisteredEvent;
import com.villo.truco.auth.domain.model.user.valueobjects.HashedPassword;
import com.villo.truco.auth.domain.model.user.valueobjects.RawPassword;
import com.villo.truco.auth.domain.model.user.valueobjects.Username;
import com.villo.truco.auth.domain.ports.PasswordHasher;
import com.villo.truco.domain.shared.AggregateBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.List;
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
    this.addDomainEvent(new UserRegisteredEvent(id, username, Instant.now()));
  }

  static User reconstruct(final PlayerId id, final Username username,
      final HashedPassword hashedPassword) {

    return new User(id, username, hashedPassword);
  }

  public List<AuthDomainEvent> getAuthDomainEvents() {

    return getDomainEvents().stream().map(AuthDomainEvent.class::cast).toList();
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
