package com.villo.truco.auth.domain.model.auth;

import com.villo.truco.auth.domain.model.auth.exceptions.InvalidUserSessionRefreshException;
import com.villo.truco.auth.domain.model.auth.valueobjects.UserSessionId;
import com.villo.truco.domain.shared.AggregateBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class UserSession extends AggregateBase<UserSessionId> {

  private final PlayerId userId;
  private final List<RefreshToken> refreshTokens;

  private UserSession(final UserSessionId id, final PlayerId userId,
      final List<RefreshToken> refreshTokens) {

    super(id);
    this.userId = Objects.requireNonNull(userId);
    this.refreshTokens = new ArrayList<>(Objects.requireNonNull(refreshTokens));
    validateState();
  }

  public static UserSession issue(final PlayerId userId, final String tokenHash,
      final Instant issuedAt, final Instant expiresAt) {

    final var initialToken = RefreshToken.issue(userId, tokenHash, issuedAt, expiresAt);
    return new UserSession(initialToken.getId().toUserSessionId(), userId, List.of(initialToken));
  }

  static UserSession rehydrate(final UserSessionId id, final PlayerId userId,
      final List<RefreshToken> refreshTokens) {

    return new UserSession(id, userId, refreshTokens);
  }

  public PlayerId userId() {

    return this.userId;
  }

  public boolean containsRefreshTokenHash(final String tokenHash) {

    return this.findTokenByHash(tokenHash) != null;
  }

  public boolean isRevoked() {

    return this.refreshTokens.stream().allMatch(RefreshToken::isRevoked);
  }

  public RefreshToken rotate(final String providedTokenHash, final String replacementTokenHash,
      final Instant rotatedAt, final Instant replacementExpiresAt) {

    final var currentToken = this.findTokenByHash(providedTokenHash);
    if (currentToken == null) {
      throw new InvalidUserSessionRefreshException();
    }

    if (!currentToken.canBeRefreshedAt(rotatedAt)) {
      this.revoke(rotatedAt);
      throw new InvalidUserSessionRefreshException();
    }

    final var replacement = RefreshToken.issue(this.userId, replacementTokenHash, rotatedAt,
        replacementExpiresAt);
    currentToken.rotateTo(replacement, rotatedAt);
    this.refreshTokens.add(replacement);
    return replacement;
  }

  public void revoke(final Instant revokedAt) {

    this.refreshTokens.forEach(token -> token.revoke(revokedAt));
  }

  public UserSessionSnapshot snapshot() {

    return new UserSessionSnapshot(this.getId(), this.userId(),
        this.refreshTokens.stream().map(RefreshToken::snapshot).toList());
  }

  public List<RefreshTokenSnapshot> refreshTokenSnapshots() {

    return this.refreshTokens.stream().map(RefreshToken::snapshot).toList();
  }

  private RefreshToken findTokenByHash(final String tokenHash) {

    return this.refreshTokens.stream().filter(token -> token.matchesTokenHash(tokenHash))
        .findFirst().orElse(null);
  }

  private void validateState() {

    if (this.refreshTokens.isEmpty()) {
      throw new IllegalArgumentException("User session must contain at least one refresh token");
    }

    this.refreshTokens.forEach(token -> {
      if (!this.userId.equals(token.userId())) {
        throw new IllegalArgumentException("All refresh tokens must belong to the same user");
      }
    });

    if (!this.getId().value().equals(this.refreshTokens.getFirst().getId().value())) {
      throw new IllegalArgumentException("User session id must match the root refresh token id");
    }
  }

}
