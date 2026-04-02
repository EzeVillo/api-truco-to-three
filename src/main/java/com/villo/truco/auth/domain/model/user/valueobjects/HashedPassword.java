package com.villo.truco.auth.domain.model.user.valueobjects;

import com.villo.truco.auth.domain.ports.PasswordHasher;
import java.util.Objects;

public record HashedPassword(String value) {

  public HashedPassword {

    Objects.requireNonNull(value, "value must not be null");
  }

  public boolean matches(final String rawPassword, final PasswordHasher passwordHasher) {

    return passwordHasher.matches(rawPassword, this);
  }

}
