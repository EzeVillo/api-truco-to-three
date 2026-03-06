package com.villo.truco.infrastructure.security;

import com.villo.truco.application.ports.PlayerTokenProvider;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import java.time.Instant;
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
public final class JwtPlayerTokenProvider implements PlayerTokenProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(JwtPlayerTokenProvider.class);

  private final JwtEncoder jwtEncoder;
  private final TrucoSecurityProperties securityProperties;

  public JwtPlayerTokenProvider(final JwtEncoder jwtEncoder,
      final TrucoSecurityProperties securityProperties) {

    this.jwtEncoder = jwtEncoder;
    this.securityProperties = securityProperties;
  }

  @Override
  public String generateAccessToken(final MatchId matchId, final PlayerId playerId) {

    final var now = Instant.now();
    final var claims = JwtClaimsSet.builder().issuer(this.securityProperties.issuer())
        .subject(playerId.value().toString()).audience(List.of(this.securityProperties.audience()))
        .issuedAt(now)
        .expiresAt(now.plusSeconds(this.securityProperties.accessTokenExpirationSeconds()))
        .claim("matchId", matchId.value().toString()).build();

    final var jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();

    final var token = this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims))
        .getTokenValue();
    LOGGER.debug("Access token generated: matchId={}, playerId={}, expiresInSeconds={}", matchId,
        playerId, this.securityProperties.accessTokenExpirationSeconds());
    return token;
  }

}
