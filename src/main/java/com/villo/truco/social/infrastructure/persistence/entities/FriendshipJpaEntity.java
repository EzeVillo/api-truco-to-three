package com.villo.truco.social.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

@Entity
@Table(name = "social_friendships")
public class FriendshipJpaEntity {

  @Id
  private UUID id;

  @Column(name = "requester_id", nullable = false)
  private UUID requesterId;

  @Column(name = "addressee_id", nullable = false)
  private UUID addresseeId;

  @Column(nullable = false)
  private String status;

  @Version
  private int version;

  public UUID getId() {

    return this.id;
  }

  public void setId(final UUID id) {

    this.id = id;
  }

  public UUID getRequesterId() {

    return this.requesterId;
  }

  public void setRequesterId(final UUID requesterId) {

    this.requesterId = requesterId;
  }

  public UUID getAddresseeId() {

    return this.addresseeId;
  }

  public void setAddresseeId(final UUID addresseeId) {

    this.addresseeId = addresseeId;
  }

  public String getStatus() {

    return this.status;
  }

  public void setStatus(final String status) {

    this.status = status;
  }

  public int getVersion() {

    return this.version;
  }

  public void setVersion(final int version) {

    this.version = version;
  }

}
