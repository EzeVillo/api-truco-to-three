package com.villo.truco.auth.application.usecases.commands;

import com.villo.truco.auth.application.ports.out.AccessTokenIssuer;
import com.villo.truco.auth.application.ports.out.RefreshTokenProvider;
import com.villo.truco.auth.domain.model.auth.UserSession;
import com.villo.truco.auth.domain.model.auth.valueobjects.UserSessionId;
import com.villo.truco.auth.domain.ports.UserSessionRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

final class AuthTestFixtures {

  static final Instant NOW = Instant.parse("2026-03-30T21:00:00Z");
  static final long USER_ACCESS_TOKEN_EXPIRES_IN = 900;
  static final long REFRESH_TOKEN_EXPIRES_IN = 2592000;

  private AuthTestFixtures() {

  }

  static Clock fixedClock() {

    return Clock.fixed(NOW, ZoneOffset.UTC);
  }

  static AccessTokenIssuer stubAccessTokenIssuer() {

    return new AccessTokenIssuer() {

      @Override
      public IssuedAccessToken issueForUser(final PlayerId playerId) {

        return new IssuedAccessToken("access-" + playerId.value(), USER_ACCESS_TOKEN_EXPIRES_IN);
      }

      @Override
      public IssuedAccessToken issueForGuest(final PlayerId playerId) {

        return new IssuedAccessToken("guest-" + playerId.value(), 604800);
      }
    };
  }

  static RefreshTokenProvider constantRefreshTokenProvider(final String tokenValue) {

    return new RefreshTokenProvider() {

      @Override
      public IssuedRefreshToken issue(final Instant issuedAt) {

        return new IssuedRefreshToken(tokenValue, this.hash(tokenValue),
            issuedAt.plusSeconds(REFRESH_TOKEN_EXPIRES_IN), REFRESH_TOKEN_EXPIRES_IN);
      }

      @Override
      public String hash(final String rawToken) {

        return "hash-" + rawToken;
      }
    };
  }

  static final class SequencedRefreshTokenProvider implements RefreshTokenProvider {

    private final AtomicInteger counter;

    SequencedRefreshTokenProvider(final int initialValue) {

      this.counter = new AtomicInteger(initialValue);
    }

    @Override
    public IssuedRefreshToken issue(final Instant issuedAt) {

      final var value = "refresh-" + this.counter.getAndIncrement();
      return new IssuedRefreshToken(value, this.hash(value),
          issuedAt.plusSeconds(REFRESH_TOKEN_EXPIRES_IN), REFRESH_TOKEN_EXPIRES_IN);
    }

    @Override
    public String hash(final String rawToken) {

      return "hash-" + rawToken;
    }

  }

  static final class InMemoryUserSessionRepository implements UserSessionRepository {

    private final Map<UserSessionId, UserSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(final UserSession session) {

      this.sessions.put(session.getId(), session);
    }

    @Override
    public Optional<UserSession> findById(final UserSessionId id) {

      return Optional.ofNullable(this.sessions.get(id));
    }

    @Override
    public Optional<UserSession> findByRefreshTokenHash(final String tokenHash) {

      return this.sessions.values().stream()
          .filter(session -> session.containsRefreshTokenHash(tokenHash)).findFirst();
    }

  }

}
