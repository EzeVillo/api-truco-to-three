package com.villo.truco.domain.model.match.valueobjects;

import java.security.SecureRandom;
import java.util.Objects;

public record InviteCode(String value) {

  private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  private static final int CODE_LENGTH = 8;
  private static final SecureRandom RANDOM = new SecureRandom();

  public InviteCode {

    Objects.requireNonNull(value, "InviteCode cannot be null");
  }

  public static InviteCode generate() {

    final var sb = new StringBuilder(CODE_LENGTH);
    for (int i = 0; i < CODE_LENGTH; i++) {
      sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
    }
    return new InviteCode(sb.toString());
  }

  public static InviteCode of(final String value) {

    Objects.requireNonNull(value, "InviteCode cannot be null");
    return new InviteCode(value);
  }

}
