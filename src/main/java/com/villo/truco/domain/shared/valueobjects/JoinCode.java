package com.villo.truco.domain.shared.valueobjects;

import java.security.SecureRandom;
import java.util.Objects;

public record JoinCode(String value) {

  private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  private static final int CODE_LENGTH = 8;
  private static final SecureRandom RANDOM = new SecureRandom();

  public JoinCode {

    Objects.requireNonNull(value, "JoinCode cannot be null");
  }

  public static JoinCode generate() {

    final var sb = new StringBuilder(CODE_LENGTH);
    for (int i = 0; i < CODE_LENGTH; i++) {
      sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
    }
    return new JoinCode(sb.toString());
  }

  public static JoinCode of(final String value) {

    Objects.requireNonNull(value, "JoinCode cannot be null");
    return new JoinCode(value);
  }

}
