package com.villo.truco.infrastructure.security;

import com.villo.truco.application.exceptions.UnauthorizedAccessException;
import com.villo.truco.application.ports.PlayerIdentity;
import com.villo.truco.application.ports.SessionGrantProvider;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class JwtSessionGrantProvider implements SessionGrantProvider {

  private static final String ALGORITHM = "HmacSHA256";
  private static final String HEADER = Base64.getUrlEncoder().withoutPadding()
      .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

  private static final String TYPE_GRANT = "grant";

  private final byte[] secretKey;
  private final long grantExpirationSeconds;
  private final ConcurrentHashMap<String, Boolean> consumedGrants = new ConcurrentHashMap<>();

  public JwtSessionGrantProvider(@Value("${truco.security.jwt-secret}") final String secret,
      @Value("${truco.security.session-grant-expiration-seconds:60}") final long grantExpirationSeconds) {

    this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
    this.grantExpirationSeconds = grantExpirationSeconds;
  }

  @Override
  public String generateGrant(final MatchId matchId, final PlayerId playerId) {

    try {
      final var jti = UUID.randomUUID().toString();
      final var now = Instant.now();
      final var payloadJson =
          "{\"matchId\":\"" + matchId.value() + "\"," + "\"playerId\":\"" + playerId.value() + "\","
              + "\"type\":\"" + TYPE_GRANT + "\"," + "\"jti\":\"" + jti + "\"," + "\"iat\":"
              + now.getEpochSecond() + "," + "\"exp\":" + now.plusSeconds(
              this.grantExpirationSeconds).getEpochSecond() + "}";

      final var encodedPayload = Base64.getUrlEncoder().withoutPadding()
          .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

      final var signature = this.sign(HEADER + "." + encodedPayload);

      return HEADER + "." + encodedPayload + "." + signature;
    } catch (final Exception e) {
      throw new RuntimeException("Failed to generate session grant", e);
    }
  }

  @Override
  public PlayerIdentity validateAndConsumeGrant(final String grant) {

    if (grant == null || grant.isBlank()) {
      throw new UnauthorizedAccessException("Missing session grant");
    }

    final var parts = grant.split("\\.");
    if (parts.length != 3) {
      throw new UnauthorizedAccessException("Invalid grant format");
    }

    final var expectedSignature = this.sign(parts[0] + "." + parts[1]);
    if (!expectedSignature.equals(parts[2])) {
      throw new UnauthorizedAccessException("Invalid grant signature");
    }

    try {
      final var payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]),
          StandardCharsets.UTF_8);

      final var type = this.extractJsonString(payloadJson, "type");
      if (!TYPE_GRANT.equals(type)) {
        throw new UnauthorizedAccessException("Invalid grant type");
      }

      final var exp = this.extractJsonLong(payloadJson, "exp");
      if (Instant.now().getEpochSecond() > exp) {
        throw new UnauthorizedAccessException("Session grant has expired");
      }

      final var jti = this.extractJsonString(payloadJson, "jti");
      if (this.consumedGrants.putIfAbsent(jti, Boolean.TRUE) != null) {
        throw new UnauthorizedAccessException("Session grant has already been used");
      }

      final var matchId = this.extractJsonString(payloadJson, "matchId");
      final var playerId = this.extractJsonString(payloadJson, "playerId");

      return new PlayerIdentity(new MatchId(UUID.fromString(matchId)),
          new PlayerId(UUID.fromString(playerId)));
    } catch (final UnauthorizedAccessException e) {
      throw e;
    } catch (final Exception e) {
      throw new UnauthorizedAccessException("Invalid session grant");
    }
  }

  private String sign(final String data) {

    try {
      final var mac = Mac.getInstance(ALGORITHM);
      mac.init(new SecretKeySpec(this.secretKey, ALGORITHM));
      final var signatureBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
    } catch (final Exception e) {
      throw new RuntimeException("Failed to sign grant", e);
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
