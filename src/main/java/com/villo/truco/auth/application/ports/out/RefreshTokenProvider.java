package com.villo.truco.auth.application.ports.out;

import java.time.Instant;

public interface RefreshTokenProvider {

  IssuedRefreshToken issue(Instant issuedAt);

  String hash(String rawToken);

  record IssuedRefreshToken(String value, String hash, Instant expiresAt, long expiresIn) {

  }

}
