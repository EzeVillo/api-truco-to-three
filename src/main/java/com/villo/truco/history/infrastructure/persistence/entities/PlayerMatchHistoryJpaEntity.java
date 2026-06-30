package com.villo.truco.history.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "player_match_history")
public class PlayerMatchHistoryJpaEntity {

  @Id
  @Column(name = "player_id", nullable = false)
  private UUID playerId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "state", columnDefinition = "jsonb", nullable = false)
  private PlayerMatchHistoryStateData state;

  @Version
  private int version;

  public UUID getPlayerId() {

    return this.playerId;
  }

  public void setPlayerId(final UUID playerId) {

    this.playerId = playerId;
  }

  public PlayerMatchHistoryStateData getState() {

    return this.state;
  }

  public void setState(final PlayerMatchHistoryStateData state) {

    this.state = state;
  }

  public int getVersion() {

    return this.version;
  }

  public void setVersion(final int version) {

    this.version = version;
  }

}
