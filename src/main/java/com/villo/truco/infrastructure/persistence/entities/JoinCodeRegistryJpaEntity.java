package com.villo.truco.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "join_code_registry")
public class JoinCodeRegistryJpaEntity {

  @Id
  @Column(name = "join_code", nullable = false)
  private String joinCode;

  @Column(name = "target_type", nullable = false)
  private String targetType;

  @Column(name = "target_id", nullable = false)
  private UUID targetId;

  public String getJoinCode() {

    return this.joinCode;
  }

  public void setJoinCode(final String joinCode) {

    this.joinCode = joinCode;
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

}
