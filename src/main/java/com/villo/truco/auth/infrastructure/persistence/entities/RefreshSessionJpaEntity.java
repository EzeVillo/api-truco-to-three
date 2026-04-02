package com.villo.truco.auth.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_sessions")
public class RefreshSessionJpaEntity {

  @Id
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "rotated_at")
  private Instant rotatedAt;

  @Column(name = "replaced_by_session_id")
  private UUID replacedBySessionId;

  public UUID getId() {

    return this.id;
  }

  public void setId(final UUID id) {

    this.id = id;
  }

  public UUID getUserId() {

    return this.userId;
  }

  public void setUserId(final UUID userId) {

    this.userId = userId;
  }

  public String getTokenHash() {

    return this.tokenHash;
  }

  public void setTokenHash(final String tokenHash) {

    this.tokenHash = tokenHash;
  }

  public Instant getExpiresAt() {

    return this.expiresAt;
  }

  public void setExpiresAt(final Instant expiresAt) {

    this.expiresAt = expiresAt;
  }

  public Instant getCreatedAt() {

    return this.createdAt;
  }

  public void setCreatedAt(final Instant createdAt) {

    this.createdAt = createdAt;
  }

  public Instant getRevokedAt() {

    return this.revokedAt;
  }

  public void setRevokedAt(final Instant revokedAt) {

    this.revokedAt = revokedAt;
  }

  public Instant getRotatedAt() {

    return this.rotatedAt;
  }

  public void setRotatedAt(final Instant rotatedAt) {

    this.rotatedAt = rotatedAt;
  }

  public UUID getReplacedBySessionId() {

    return this.replacedBySessionId;
  }

  public void setReplacedBySessionId(final UUID replacedBySessionId) {

    this.replacedBySessionId = replacedBySessionId;
  }

}
