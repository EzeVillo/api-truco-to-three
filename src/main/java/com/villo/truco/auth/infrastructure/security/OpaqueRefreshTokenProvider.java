package com.villo.truco.auth.infrastructure.security;

import com.villo.truco.auth.application.ports.out.RefreshTokenProvider;
import com.villo.truco.infrastructure.security.TrucoSecurityProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public final class OpaqueRefreshTokenProvider implements RefreshTokenProvider {

  private static final int TOKEN_BYTES = 32;

  private final SecureRandom secureRandom = new SecureRandom();
  private final TrucoSecurityProperties securityProperties;

  public OpaqueRefreshTokenProvider(final TrucoSecurityProperties securityProperties) {

    this.securityProperties = securityProperties;
  }

  @Override
  public IssuedRefreshToken issue(final Instant issuedAt) {

    final var bytes = new byte[TOKEN_BYTES];
    this.secureRandom.nextBytes(bytes);
    final var value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    return new IssuedRefreshToken(value, this.hash(value),
        issuedAt.plusSeconds(this.securityProperties.refreshTokenExpirationSeconds()),
        this.securityProperties.refreshTokenExpirationSeconds());
  }

  @Override
  public String hash(final String rawToken) {

    try {
      final var digest = MessageDigest.getInstance("SHA-256");
      final var hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (final NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 algorithm is not available", ex);
    }
  }

}
