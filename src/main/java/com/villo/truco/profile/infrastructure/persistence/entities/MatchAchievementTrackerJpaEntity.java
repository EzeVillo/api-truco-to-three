package com.villo.truco.profile.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "match_achievement_trackers")
public class MatchAchievementTrackerJpaEntity {

  @Id
  @Column(name = "match_id", nullable = false)
  private UUID matchId;

  @Column(name = "player_one_id", nullable = false)
  private UUID playerOneId;

  @Column(name = "player_two_id", nullable = false)
  private UUID playerTwoId;

  @Column(name = "human_vs_human", nullable = false)
  private boolean humanVsHuman;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "state", columnDefinition = "jsonb", nullable = false)
  private MatchAchievementTrackerStateData state;

  @Version
  private int version;

  public UUID getMatchId() {

    return this.matchId;
  }

  public void setMatchId(final UUID matchId) {

    this.matchId = matchId;
  }

  public UUID getPlayerOneId() {

    return this.playerOneId;
  }

  public void setPlayerOneId(final UUID playerOneId) {

    this.playerOneId = playerOneId;
  }

  public UUID getPlayerTwoId() {

    return this.playerTwoId;
  }

  public void setPlayerTwoId(final UUID playerTwoId) {

    this.playerTwoId = playerTwoId;
  }

  public boolean isHumanVsHuman() {

    return this.humanVsHuman;
  }

  public void setHumanVsHuman(final boolean humanVsHuman) {

    this.humanVsHuman = humanVsHuman;
  }

  public MatchAchievementTrackerStateData getState() {

    return this.state;
  }

  public void setState(final MatchAchievementTrackerStateData state) {

    this.state = state;
  }

  public int getVersion() {

    return this.version;
  }

  public void setVersion(final int version) {

    this.version = version;
  }
}
