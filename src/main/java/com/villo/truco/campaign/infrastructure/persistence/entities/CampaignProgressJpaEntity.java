package com.villo.truco.campaign.infrastructure.persistence.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "campaign_progress")
public class CampaignProgressJpaEntity {

  @Id
  @Column(name = "player_id", nullable = false)
  private UUID playerId;

  @Column(name = "points", nullable = false)
  private int points;

  @Column(name = "active_challenge_match_id")
  private UUID activeChallengeMatchId;

  @Column(name = "active_challenge_rival_id")
  private UUID activeChallengeRivalId;

  @Column(name = "top_one_reached", nullable = false)
  private boolean topOneReached;

  @Column(name = "all_rivals_defeated", nullable = false)
  private boolean allRivalsDefeated;

  @Column(name = "all_casual_bots_unlocked", nullable = false)
  private boolean allCasualBotsUnlocked;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "campaign_rival_records", joinColumns = @JoinColumn(name = "player_id"), uniqueConstraints = @UniqueConstraint(columnNames = {
      "player_id", "rival_id"}))
  private List<CampaignRivalRecordJpaEmbeddable> rivalRecords = new ArrayList<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "campaign_unlocked_casual_bots", joinColumns = @JoinColumn(name = "player_id"))
  @Column(name = "bot_id", nullable = false)
  private Set<UUID> unlockedCasualBots = new LinkedHashSet<>();

  @Version
  private int version;

  public UUID getPlayerId() {

    return this.playerId;
  }

  public void setPlayerId(final UUID playerId) {

    this.playerId = playerId;
  }

  public int getPoints() {

    return this.points;
  }

  public void setPoints(final int points) {

    this.points = points;
  }

  public UUID getActiveChallengeMatchId() {

    return this.activeChallengeMatchId;
  }

  public void setActiveChallengeMatchId(final UUID activeChallengeMatchId) {

    this.activeChallengeMatchId = activeChallengeMatchId;
  }

  public UUID getActiveChallengeRivalId() {

    return this.activeChallengeRivalId;
  }

  public void setActiveChallengeRivalId(final UUID activeChallengeRivalId) {

    this.activeChallengeRivalId = activeChallengeRivalId;
  }

  public boolean isTopOneReached() {

    return this.topOneReached;
  }

  public void setTopOneReached(final boolean topOneReached) {

    this.topOneReached = topOneReached;
  }

  public boolean isAllRivalsDefeated() {

    return this.allRivalsDefeated;
  }

  public void setAllRivalsDefeated(final boolean allRivalsDefeated) {

    this.allRivalsDefeated = allRivalsDefeated;
  }

  public boolean isAllCasualBotsUnlocked() {

    return this.allCasualBotsUnlocked;
  }

  public void setAllCasualBotsUnlocked(final boolean allCasualBotsUnlocked) {

    this.allCasualBotsUnlocked = allCasualBotsUnlocked;
  }

  public List<CampaignRivalRecordJpaEmbeddable> getRivalRecords() {

    return this.rivalRecords;
  }

  public void setRivalRecords(final List<CampaignRivalRecordJpaEmbeddable> rivalRecords) {

    this.rivalRecords = rivalRecords;
  }

  public Set<UUID> getUnlockedCasualBots() {

    return this.unlockedCasualBots;
  }

  public void setUnlockedCasualBots(final Set<UUID> unlockedCasualBots) {

    this.unlockedCasualBots = unlockedCasualBots;
  }

  public int getVersion() {

    return this.version;
  }

  public void setVersion(final int version) {

    this.version = version;
  }

}
