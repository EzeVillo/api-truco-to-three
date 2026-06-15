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
  @Column(name = "match_state_before", columnDefinition = "jsonb", nullable = false)
  private JsonNode matchStateBefore;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "match_state_after", columnDefinition = "jsonb", nullable = false)
  private JsonNode matchStateAfter;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "decision_context", columnDefinition = "jsonb", nullable = false)
  private JsonNode decisionContext;

  @Column(name = "score_actor_before", nullable = false)
  private int scoreActorBefore;

  @Column(name = "score_actor_after", nullable = false)
  private int scoreActorAfter;

  @Column(name = "score_opp_before", nullable = false)
  private int scoreOppBefore;

  @Column(name = "score_opp_after", nullable = false)
  private int scoreOppAfter;

  @Column(name = "tantos_actor", nullable = false)
  private int tantosActor;

  @Column(name = "tantos_opp", nullable = false)
  private int tantosOpp;

  @Column(name = "is_mano", nullable = false)
  private boolean mano;

  @Column(name = "forced", nullable = false)
  private boolean forced;

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

  public JsonNode getMatchStateBefore() {

    return this.matchStateBefore;
  }

  public void setMatchStateBefore(final JsonNode matchStateBefore) {

    this.matchStateBefore = matchStateBefore;
  }

  public JsonNode getMatchStateAfter() {

    return this.matchStateAfter;
  }

  public void setMatchStateAfter(final JsonNode matchStateAfter) {

    this.matchStateAfter = matchStateAfter;
  }

  public JsonNode getDecisionContext() {

    return this.decisionContext;
  }

  public void setDecisionContext(final JsonNode decisionContext) {

    this.decisionContext = decisionContext;
  }

  public int getScoreActorBefore() {

    return this.scoreActorBefore;
  }

  public void setScoreActorBefore(final int scoreActorBefore) {

    this.scoreActorBefore = scoreActorBefore;
  }

  public int getScoreActorAfter() {

    return this.scoreActorAfter;
  }

  public void setScoreActorAfter(final int scoreActorAfter) {

    this.scoreActorAfter = scoreActorAfter;
  }

  public int getScoreOppBefore() {

    return this.scoreOppBefore;
  }

  public void setScoreOppBefore(final int scoreOppBefore) {

    this.scoreOppBefore = scoreOppBefore;
  }

  public int getScoreOppAfter() {

    return this.scoreOppAfter;
  }

  public void setScoreOppAfter(final int scoreOppAfter) {

    this.scoreOppAfter = scoreOppAfter;
  }

  public int getTantosActor() {

    return this.tantosActor;
  }

  public void setTantosActor(final int tantosActor) {

    this.tantosActor = tantosActor;
  }

  public int getTantosOpp() {

    return this.tantosOpp;
  }

  public void setTantosOpp(final int tantosOpp) {

    this.tantosOpp = tantosOpp;
  }

  public boolean isMano() {

    return this.mano;
  }

  public void setMano(final boolean mano) {

    this.mano = mano;
  }

  public boolean isForced() {

    return this.forced;
  }

  public void setForced(final boolean forced) {

    this.forced = forced;
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
