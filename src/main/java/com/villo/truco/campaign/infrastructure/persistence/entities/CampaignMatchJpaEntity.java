package com.villo.truco.campaign.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "campaign_matches")
public class CampaignMatchJpaEntity {

  @Id
  @Column(name = "match_id", nullable = false)
  private UUID matchId;

  @Column(name = "player_id", nullable = false)
  private UUID playerId;

  public UUID getMatchId() {

    return this.matchId;
  }

  public void setMatchId(final UUID matchId) {

    this.matchId = matchId;
  }

  public UUID getPlayerId() {

    return this.playerId;
  }

  public void setPlayerId(final UUID playerId) {

    this.playerId = playerId;
  }

}
