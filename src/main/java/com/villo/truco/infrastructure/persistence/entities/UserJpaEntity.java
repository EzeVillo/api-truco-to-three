package com.villo.truco.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserJpaEntity {

  @Id
  private UUID id;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(name = "hashed_password", nullable = false)
  private String hashedPassword;

  public UserJpaEntity() {

  }

  public UUID getId() {

    return id;
  }

  public void setId(UUID id) {

    this.id = id;
  }

  public String getUsername() {

    return username;
  }

  public void setUsername(String username) {

    this.username = username;
  }

  public String getHashedPassword() {

    return hashedPassword;
  }

  public void setHashedPassword(String hashedPassword) {

    this.hashedPassword = hashedPassword;
  }

}
