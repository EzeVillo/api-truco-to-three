package com.villo.truco.infrastructure.persistence.entities;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "match_action_log", uniqueConstraints = @UniqueConstraint(name = "uq_match_action_log_match_state_version", columnNames = {
    "match_id", "state_version"}))
public class MatchActionLogJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "match_id", nullable = false)
  private UUID matchId;

  @Column(name = "state_version", nullable = false)
  private long stateVersion;

  @Column(name = "game_number", nullable = false)
  private int gameNumber;

  @Column(name = "round_number", nullable = false)
  private int roundNumber;

  @Column(name = "actor_seat", nullable = false)
  private String actorSeat;

  @Column(name = "actor_type", nullable = false)
  private String actorType;

  @Column(name = "action_type", nullable = false)
  private String actionType;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "action_detail", columnDefinition = "jsonb")
  private JsonNode actionDetail;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "match_state", columnDefinition = "jsonb", nullable = false)
  private JsonNode matchState;

  @Column(name = "schema_version", nullable = false)
  private int schemaVersion;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "recorded_at", nullable = false)
  private Instant recordedAt;

  @PrePersist
  void prePersist() {

    if (this.recordedAt == null) {
      this.recordedAt = Instant.now();
    }
  }

  public Long getId() {

    return this.id;
  }

  public UUID getMatchId() {

    return this.matchId;
  }

  public void setMatchId(final UUID matchId) {

    this.matchId = matchId;
  }

  public long getStateVersion() {

    return this.stateVersion;
  }

  public void setStateVersion(final long stateVersion) {

    this.stateVersion = stateVersion;
  }

  public int getGameNumber() {

    return this.gameNumber;
  }

  public void setGameNumber(final int gameNumber) {

    this.gameNumber = gameNumber;
  }

  public int getRoundNumber() {

    return this.roundNumber;
  }

  public void setRoundNumber(final int roundNumber) {

    this.roundNumber = roundNumber;
  }

  public String getActorSeat() {

    return this.actorSeat;
  }

  public void setActorSeat(final String actorSeat) {

    this.actorSeat = actorSeat;
  }

  public String getActorType() {

    return this.actorType;
  }

  public void setActorType(final String actorType) {

    this.actorType = actorType;
  }

  public String getActionType() {

    return this.actionType;
  }

  public void setActionType(final String actionType) {

    this.actionType = actionType;
  }

  public JsonNode getActionDetail() {

    return this.actionDetail;
  }

  public void setActionDetail(final JsonNode actionDetail) {

    this.actionDetail = actionDetail;
  }

  public JsonNode getMatchState() {

    return this.matchState;
  }

  public void setMatchState(final JsonNode matchState) {

    this.matchState = matchState;
  }

  public int getSchemaVersion() {

    return this.schemaVersion;
  }

  public void setSchemaVersion(final int schemaVersion) {

    this.schemaVersion = schemaVersion;
  }

  public Instant getOccurredAt() {

    return this.occurredAt;
  }

  public void setOccurredAt(final Instant occurredAt) {

    this.occurredAt = occurredAt;
  }

  public Instant getRecordedAt() {

    return this.recordedAt;
  }

  public void setRecordedAt(final Instant recordedAt) {

    this.recordedAt = recordedAt;
  }

}
