package com.villo.truco.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rematch_sessions")
public class RematchSessionJpaEntity {

  @Id
  private UUID id;

  @Column(name = "origin_match_id", nullable = false, unique = true)
  private UUID originMatchId;

  @Column(name = "player_one_id", nullable = false)
  private UUID playerOneId;

  @Column(name = "player_two_id", nullable = false)
  private UUID playerTwoId;

  @Column(name = "player_one_choice", nullable = false, length = 20)
  private String playerOneChoice;

  @Column(name = "player_two_choice", nullable = false, length = 20)
  private String playerTwoChoice;

  @Column(name = "player_one_is_bot", nullable = false)
  private boolean playerOneIsBot;

  @Column(name = "player_two_is_bot", nullable = false)
  private boolean playerTwoIsBot;

  @Column(name = "status", nullable = false, length = 20)
  private String status;

  @Column(name = "games_to_win", nullable = false)
  private int gamesToWin;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "result_match_id")
  private UUID resultMatchId;

  @Version
  private int version;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  public RematchSessionJpaEntity() {

  }

  @PrePersist
  void onPrePersist() {

    this.createdAt = Instant.now();
  }

  @PreUpdate
  void onPreUpdate() {

    this.updatedAt = Instant.now();
  }

  public UUID getId() {

    return id;
  }

  public void setId(final UUID id) {

    this.id = id;
  }

  public UUID getOriginMatchId() {

    return originMatchId;
  }

  public void setOriginMatchId(final UUID originMatchId) {

    this.originMatchId = originMatchId;
  }

  public UUID getPlayerOneId() {

    return playerOneId;
  }

  public void setPlayerOneId(final UUID playerOneId) {

    this.playerOneId = playerOneId;
  }

  public UUID getPlayerTwoId() {

    return playerTwoId;
  }

  public void setPlayerTwoId(final UUID playerTwoId) {

    this.playerTwoId = playerTwoId;
  }

  public String getPlayerOneChoice() {

    return playerOneChoice;
  }

  public void setPlayerOneChoice(final String playerOneChoice) {

    this.playerOneChoice = playerOneChoice;
  }

  public String getPlayerTwoChoice() {

    return playerTwoChoice;
  }

  public void setPlayerTwoChoice(final String playerTwoChoice) {

    this.playerTwoChoice = playerTwoChoice;
  }

  public boolean isPlayerOneIsBot() {

    return playerOneIsBot;
  }

  public void setPlayerOneIsBot(final boolean playerOneIsBot) {

    this.playerOneIsBot = playerOneIsBot;
  }

  public boolean isPlayerTwoIsBot() {

    return playerTwoIsBot;
  }

  public void setPlayerTwoIsBot(final boolean playerTwoIsBot) {

    this.playerTwoIsBot = playerTwoIsBot;
  }

  public String getStatus() {

    return status;
  }

  public void setStatus(final String status) {

    this.status = status;
  }

  public int getGamesToWin() {

    return gamesToWin;
  }

  public void setGamesToWin(final int gamesToWin) {

    this.gamesToWin = gamesToWin;
  }

  public Instant getExpiresAt() {

    return expiresAt;
  }

  public void setExpiresAt(final Instant expiresAt) {

    this.expiresAt = expiresAt;
  }

  public UUID getResultMatchId() {

    return resultMatchId;
  }

  public void setResultMatchId(final UUID resultMatchId) {

    this.resultMatchId = resultMatchId;
  }

  public int getVersion() {

    return version;
  }

  public void setVersion(final int version) {

    this.version = version;
  }

  public Instant getCreatedAt() {

    return createdAt;
  }

  public Instant getUpdatedAt() {

    return updatedAt;
  }

}
