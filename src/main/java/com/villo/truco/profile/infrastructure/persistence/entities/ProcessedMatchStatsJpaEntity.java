package com.villo.truco.profile.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_match_stats")
@IdClass(ProcessedMatchStatsId.class)
public class ProcessedMatchStatsJpaEntity {

  @Id
  @Column(name = "player_id", nullable = false)
  private UUID playerId;

  @Id
  @Column(name = "match_id", nullable = false)
  private UUID matchId;

  @Column(name = "processed_at", nullable = false)
  private Instant processedAt;

  public UUID getPlayerId() {

    return this.playerId;
  }

  public void setPlayerId(final UUID playerId) {

    this.playerId = playerId;
  }

  public UUID getMatchId() {

    return this.matchId;
  }

  public void setMatchId(final UUID matchId) {

    this.matchId = matchId;
  }

  public Instant getProcessedAt() {

    return this.processedAt;
  }

  public void setProcessedAt(final Instant processedAt) {

    this.processedAt = processedAt;
  }

}
