package com.villo.truco.auth.application.usecases.commands;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

final class AuthTestFixtures {

  static final Instant NOW = Instant.parse("2026-03-30T21:00:00Z");
  static final long USER_ACCESS_TOKEN_EXPIRES_IN = 900;
  static final long REFRESH_TOKEN_EXPIRES_IN = 2592000;

  private AuthTestFixtures() {

  }

  static Clock fixedClock() {

    return Clock.fixed(NOW, ZoneOffset.UTC);
  }

}
