package com.villo.truco.auth.infrastructure.security;

import com.villo.truco.auth.application.ports.out.AccessTokenIssuer;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.security.TrucoSecurityProperties;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
public final class JwtAccessTokenProvider implements AccessTokenIssuer {

  private static final Logger LOGGER = LoggerFactory.getLogger(JwtAccessTokenProvider.class);

  private final JwtEncoder jwtEncoder;
  private final TrucoSecurityProperties securityProperties;
  private final Clock clock;

  public JwtAccessTokenProvider(final JwtEncoder jwtEncoder,
      final TrucoSecurityProperties securityProperties, final Clock clock) {

    this.jwtEncoder = jwtEncoder;
    this.securityProperties = securityProperties;
    this.clock = clock;
  }

  @Override
  public IssuedAccessToken issueForUser(final PlayerId playerId) {

    return this.generateAccessToken(playerId,
        this.securityProperties.userAccessTokenExpirationSeconds(), "user");
  }

  @Override
  public IssuedAccessToken issueForGuest(final PlayerId playerId) {

    return this.generateAccessToken(playerId,
        this.securityProperties.guestAccessTokenExpirationSeconds(), "guest");
  }

  private IssuedAccessToken generateAccessToken(final PlayerId playerId,
      final long expiresInSeconds, final String tokenType) {

    final var now = this.clock.instant();
    final var claims = JwtClaimsSet.builder().issuer(this.securityProperties.issuer())
        .subject(playerId.value().toString()).audience(List.of(this.securityProperties.audience()))
        .issuedAt(now).claim("token_use", tokenType).expiresAt(now.plusSeconds(expiresInSeconds))
        .build();

    final var jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();

    final var token = this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims))
        .getTokenValue();
    LOGGER.debug("Access token generated: playerId={}, tokenUse={}, expiresInSeconds={}", playerId,
        tokenType, expiresInSeconds);
    return new IssuedAccessToken(token, expiresInSeconds);
  }

}
