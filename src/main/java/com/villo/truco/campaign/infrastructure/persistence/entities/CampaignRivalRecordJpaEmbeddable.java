package com.villo.truco.campaign.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.UUID;

@Embeddable
public class CampaignRivalRecordJpaEmbeddable {

  @Column(name = "rival_id", nullable = false)
  private UUID rivalId;

  @Column(name = "wins", nullable = false)
  private int wins;

  @Column(name = "losses", nullable = false)
  private int losses;

  public UUID getRivalId() {

    return this.rivalId;
  }

  public void setRivalId(final UUID rivalId) {

    this.rivalId = rivalId;
  }

  public int getWins() {

    return this.wins;
  }

  public void setWins(final int wins) {

    this.wins = wins;
  }

  public int getLosses() {

    return this.losses;
  }

  public void setLosses(final int losses) {

    this.losses = losses;
  }

}
