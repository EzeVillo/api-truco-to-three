package com.villo.truco.auth.domain.model.auth;

import com.villo.truco.auth.domain.model.auth.valueobjects.RefreshTokenId;
import com.villo.truco.domain.shared.EntityBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.Objects;

public final class RefreshToken extends EntityBase<RefreshTokenId> {

  private final PlayerId userId;
  private final String tokenHash;
  private final Instant expiresAt;
  private final Instant createdAt;
  private Instant revokedAt;
  private Instant rotatedAt;
  private RefreshTokenId replacedByTokenId;

  private RefreshToken(final RefreshTokenId id, final PlayerId userId, final String tokenHash,
      final Instant expiresAt, final Instant createdAt, final Instant revokedAt,
      final Instant rotatedAt, final RefreshTokenId replacedByTokenId) {

    super(id);
    this.userId = Objects.requireNonNull(userId);
    this.tokenHash = Objects.requireNonNull(tokenHash);
    this.expiresAt = Objects.requireNonNull(expiresAt);
    this.createdAt = Objects.requireNonNull(createdAt);
    this.revokedAt = revokedAt;
    this.rotatedAt = rotatedAt;
    this.replacedByTokenId = replacedByTokenId;
  }

  public static RefreshToken issue(final PlayerId userId, final String tokenHash,
      final Instant createdAt, final Instant expiresAt) {

    return new RefreshToken(RefreshTokenId.generate(), userId, tokenHash, expiresAt, createdAt,
        null, null, null);
  }

  public static RefreshToken rehydrate(final RefreshTokenId id, final PlayerId userId,
      final String tokenHash, final Instant expiresAt, final Instant createdAt,
      final Instant revokedAt, final Instant rotatedAt, final RefreshTokenId replacedByTokenId) {

    return new RefreshToken(id, userId, tokenHash, expiresAt, createdAt, revokedAt, rotatedAt,
        replacedByTokenId);
  }

  public PlayerId userId() {

    return this.userId;
  }

  public String tokenHash() {

    return this.tokenHash;
  }

  public boolean canBeRefreshedAt(final Instant now) {

    return !this.isRevoked() && !this.isRotated() && this.expiresAt.isAfter(now);
  }

  public boolean matchesTokenHash(final String candidateTokenHash) {

    return this.tokenHash.equals(candidateTokenHash);
  }

  public boolean isRevoked() {

    return this.revokedAt != null;
  }

  public boolean isRotated() {

    return this.rotatedAt != null;
  }

  public void rotateTo(final RefreshToken replacement, final Instant rotatedAt) {

    Objects.requireNonNull(replacement);
    this.rotatedAt = Objects.requireNonNull(rotatedAt);
    this.replacedByTokenId = replacement.getId();
  }

  public void revoke(final Instant revokedAt) {

    if (this.revokedAt == null) {
      this.revokedAt = Objects.requireNonNull(revokedAt);
    }
  }

  public RefreshTokenSnapshot snapshot() {

    return new RefreshTokenSnapshot(this.getId(), this.userId, this.tokenHash, this.expiresAt,
        this.createdAt, this.revokedAt, this.rotatedAt, this.replacedByTokenId);

  }

}
