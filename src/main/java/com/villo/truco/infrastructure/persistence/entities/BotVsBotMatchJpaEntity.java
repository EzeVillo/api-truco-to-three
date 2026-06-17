package com.villo.truco.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "bot_vs_bot_matches")
public class BotVsBotMatchJpaEntity {

  @Id
  @Column(name = "match_id", nullable = false)
  private UUID matchId;

  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  public UUID getMatchId() {

    return this.matchId;
  }

  public void setMatchId(final UUID matchId) {

    this.matchId = matchId;
  }

  public UUID getOwnerId() {

    return this.ownerId;
  }

  public void setOwnerId(final UUID ownerId) {

    this.ownerId = ownerId;
  }

}
