package com.villo.truco.auth.domain.model.user.events;

import com.villo.truco.auth.domain.model.user.valueobjects.Username;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.Objects;

public final class UserRegisteredEvent extends AuthDomainEvent {

  private final PlayerId userId;
  private final Username username;
  private final Instant registeredAt;

  public UserRegisteredEvent(final PlayerId userId, final Username username,
      final Instant registeredAt) {

    super("USER_REGISTERED");
    this.userId = Objects.requireNonNull(userId);
    this.username = Objects.requireNonNull(username);
    this.registeredAt = Objects.requireNonNull(registeredAt);
  }

  public PlayerId getUserId() {

    return this.userId;
  }

  public Username getUsername() {

    return this.username;
  }

  public Instant getRegisteredAt() {

    return this.registeredAt;
  }

}
