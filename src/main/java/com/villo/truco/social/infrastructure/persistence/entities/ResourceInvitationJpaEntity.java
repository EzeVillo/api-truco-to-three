package com.villo.truco.social.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "social_resource_invitations")
public class ResourceInvitationJpaEntity {

  @Id
  private UUID id;

  @Column(name = "sender_id", nullable = false)
  private UUID senderId;

  @Column(name = "recipient_id", nullable = false)
  private UUID recipientId;

  @Column(name = "target_type", nullable = false)
  private String targetType;

  @Column(name = "target_id", nullable = false)
  private UUID targetId;

  @Column(nullable = false)
  private String status;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Version
  private int version;

  public UUID getId() {

    return this.id;
  }

  public void setId(final UUID id) {

    this.id = id;
  }

  public UUID getSenderId() {

    return this.senderId;
  }

  public void setSenderId(final UUID senderId) {

    this.senderId = senderId;
  }

  public UUID getRecipientId() {

    return this.recipientId;
  }

  public void setRecipientId(final UUID recipientId) {

    this.recipientId = recipientId;
  }

  public String getTargetType() {

    return this.targetType;
  }

  public void setTargetType(final String targetType) {

    this.targetType = targetType;
  }

  public UUID getTargetId() {

    return this.targetId;
  }

  public void setTargetId(final UUID targetId) {

    this.targetId = targetId;
  }

  public String getStatus() {

    return this.status;
  }

  public void setStatus(final String status) {

    this.status = status;
  }

  public Instant getExpiresAt() {

    return this.expiresAt;
  }

  public void setExpiresAt(final Instant expiresAt) {

    this.expiresAt = expiresAt;
  }

  public int getVersion() {

    return this.version;
  }

  public void setVersion(final int version) {

    this.version = version;
  }

}
