package com.villo.truco.auth.domain.model.user;

import com.villo.truco.auth.domain.model.user.exceptions.UsernameUnavailableException;
import com.villo.truco.auth.domain.model.user.valueobjects.Username;
import com.villo.truco.auth.domain.ports.UserRepository;
import java.util.Objects;

public final class UsernameAvailabilityPolicy {

  private final UserRepository userRepository;

  public UsernameAvailabilityPolicy(final UserRepository userRepository) {

    this.userRepository = Objects.requireNonNull(userRepository);
  }

  public void ensureAvailable(final Username username) {

    Objects.requireNonNull(username, "username must not be null");

    if (this.userRepository.existsByUsername(username)) {
      throw new UsernameUnavailableException(username.value());
    }
  }

}
