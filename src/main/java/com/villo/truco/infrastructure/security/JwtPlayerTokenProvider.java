package com.villo.truco.infrastructure.security;

import com.villo.truco.application.exceptions.UnauthorizedAccessException;
import com.villo.truco.application.ports.PlayerIdentity;
import com.villo.truco.application.ports.PlayerTokenProvider;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class JwtPlayerTokenProvider implements PlayerTokenProvider {

  private static final String ALGORITHM = "HmacSHA256";
  private static final String HEADER = Base64.getUrlEncoder().withoutPadding()
      .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

  private static final String TYPE_ACCESS = "access";
  private static final String TYPE_REFRESH = "refresh";

  private final byte[] secretKey;
  private final long accessExpirationSeconds;
  private final long refreshExpirationSeconds;

  public JwtPlayerTokenProvider(@Value("${truco.security.jwt-secret}") final String secret,
      @Value("${truco.security.access-token-expiration-seconds:900}") final long accessExpirationSeconds,
      @Value("${truco.security.refresh-token-expiration-seconds:86400}") final long refreshExpirationSeconds) {

    this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
    this.accessExpirationSeconds = accessExpirationSeconds;
    this.refreshExpirationSeconds = refreshExpirationSeconds;
  }

  @Override
  public String generateAccessToken(final MatchId matchId, final PlayerId playerId) {

    return this.generateToken(matchId, playerId, TYPE_ACCESS, this.accessExpirationSeconds);
  }

  @Override
  public PlayerIdentity validateAccessToken(final String token) {

    return this.validateToken(token, TYPE_ACCESS);
  }

  @Override
  public String generateRefreshToken(final MatchId matchId, final PlayerId playerId) {

    return this.generateToken(matchId, playerId, TYPE_REFRESH, this.refreshExpirationSeconds);
  }

  @Override
  public PlayerIdentity validateRefreshToken(final String token) {

    return this.validateToken(token, TYPE_REFRESH);
  }

  private String generateToken(final MatchId matchId, final PlayerId playerId, final String type,
      final long expirationSeconds) {

    try {
      final var now = Instant.now();
      final var payloadJson =
          "{\"matchId\":\"" + matchId.value() + "\"," + "\"playerId\":\"" + playerId.value() + "\","
              + "\"type\":\"" + type + "\"," + "\"iat\":" + now.getEpochSecond() + "," + "\"exp\":"
              + now.plusSeconds(expirationSeconds).getEpochSecond() + "}";

      final var encodedPayload = Base64.getUrlEncoder().withoutPadding()
          .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

      final var signature = this.sign(HEADER + "." + encodedPayload);

      return HEADER + "." + encodedPayload + "." + signature;
    } catch (final Exception e) {
      throw new RuntimeException("Failed to generate token", e);
    }
  }

  private PlayerIdentity validateToken(final String token, final String expectedType) {

    if (token == null || token.isBlank()) {
      throw new UnauthorizedAccessException("Missing authentication token");
    }

    final var parts = token.split("\\.");
    if (parts.length != 3) {
      throw new UnauthorizedAccessException("Invalid token format");
    }

    final var expectedSignature = this.sign(parts[0] + "." + parts[1]);
    if (!expectedSignature.equals(parts[2])) {
      throw new UnauthorizedAccessException("Invalid token signature");
    }

    try {
      final var payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]),
          StandardCharsets.UTF_8);

      final var type = this.extractJsonString(payloadJson, "type");
      if (!expectedType.equals(type)) {
        throw new UnauthorizedAccessException("Invalid token type");
      }

      final var exp = this.extractJsonLong(payloadJson, "exp");
      if (Instant.now().getEpochSecond() > exp) {
        throw new UnauthorizedAccessException("Token has expired");
      }

      final var matchId = this.extractJsonString(payloadJson, "matchId");
      final var playerId = this.extractJsonString(payloadJson, "playerId");

      return new PlayerIdentity(new MatchId(UUID.fromString(matchId)),
          new PlayerId(UUID.fromString(playerId)));
    } catch (final UnauthorizedAccessException e) {
      throw e;
    } catch (final Exception e) {
      throw new UnauthorizedAccessException("Invalid token");
    }
  }

  private String sign(final String data) {

    try {
      final var mac = Mac.getInstance(ALGORITHM);
      mac.init(new SecretKeySpec(this.secretKey, ALGORITHM));
      final var signatureBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
    } catch (final Exception e) {
      throw new RuntimeException("Failed to sign token", e);
    }
  }

  private String extractJsonString(final String json, final String key) {

    final var searchKey = "\"" + key + "\":\"";
    final var start = json.indexOf(searchKey) + searchKey.length();
    final var end = json.indexOf('"', start);
    return json.substring(start, end);
  }

  private long extractJsonLong(final String json, final String key) {

    final var searchKey = "\"" + key + "\":";
    final var start = json.indexOf(searchKey) + searchKey.length();
    var end = start;
    while (end < json.length() && (Character.isDigit(json.charAt(end))
        || json.charAt(end) == '-')) {
      end++;
    }
    return Long.parseLong(json.substring(start, end));
  }

}
