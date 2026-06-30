package com.villo.truco.social.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

@Entity
@Table(name = "social_preferences")
public class SocialPreferencesJpaEntity {

  @Id
  @Column(name = "player_id", nullable = false)
  private UUID playerId;

  @Column(name = "accepts_friend_requests", nullable = false)
  private boolean acceptsFriendRequests;

  @Version
  private int version;

  public UUID getPlayerId() {

    return this.playerId;
  }

  public void setPlayerId(final UUID playerId) {

    this.playerId = playerId;
  }

  public boolean isAcceptsFriendRequests() {

    return this.acceptsFriendRequests;
  }

  public void setAcceptsFriendRequests(final boolean acceptsFriendRequests) {

    this.acceptsFriendRequests = acceptsFriendRequests;
  }

  public int getVersion() {

    return this.version;
  }

  public void setVersion(final int version) {

    this.version = version;
  }

}
