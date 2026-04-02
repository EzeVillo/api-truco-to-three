package com.villo.truco.auth.domain.model.user.valueobjects;

import com.villo.truco.auth.domain.model.user.exceptions.InvalidPasswordException;
import java.util.Objects;

public record RawPassword(String value) {

  public RawPassword {

    Objects.requireNonNull(value, "value must not be null");

    if (value.length() < 5) {
      throw new InvalidPasswordException("Password must have at least 5 characters");
    }
    if (value.chars().noneMatch(Character::isDigit)) {
      throw new InvalidPasswordException("Password must contain at least 1 number");
    }
    if (value.chars().allMatch(Character::isLetterOrDigit)) {
      throw new InvalidPasswordException("Password must contain at least 1 symbol");
    }
  }

}
