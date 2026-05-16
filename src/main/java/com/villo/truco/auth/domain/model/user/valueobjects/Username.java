package com.villo.truco.auth.domain.model.user.valueobjects;

import com.villo.truco.auth.domain.model.user.exceptions.InvalidUsernameException;
import java.util.Locale;
import java.util.Objects;

public record Username(String value) {

  public Username {

    Objects.requireNonNull(value, "value must not be null");

    if (!value.chars().allMatch(Username::isAsciiLetterOrDigit)) {
      throw new InvalidUsernameException("Username must contain only ASCII letters and numbers");
    }

    if (countLetters(value) < 3) {
      throw new InvalidUsernameException("Username must contain at least 3 letters");
    }

  }

  private static int countLetters(final String username) {

    return (int) username.chars().filter(Character::isLetter).count();
  }

  private static boolean isAsciiLetterOrDigit(final int character) {

    return character >= 'A' && character <= 'Z' || character >= 'a' && character <= 'z'
        || character >= '0' && character <= '9';
  }

  public String normalized() {

    return this.value.toLowerCase(Locale.ROOT);
  }

  @Override
  public boolean equals(final Object other) {

    if (this == other) {
      return true;
    }
    if (!(other instanceof Username that)) {
      return false;
    }
    return this.normalized().equals(that.normalized());
  }

  @Override
  public int hashCode() {

    return Objects.hash(this.normalized());
  }

}
