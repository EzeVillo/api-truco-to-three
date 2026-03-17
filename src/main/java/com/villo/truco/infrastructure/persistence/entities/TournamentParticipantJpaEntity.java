package com.villo.truco.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "tournament_participants")
@IdClass(TournamentParticipantId.class)
public class TournamentParticipantJpaEntity {

  @Id
  @Column(name = "tournament_id")
  private UUID tournamentId;

  @Id
  @Column(name = "player_id")
  private UUID playerId;

  @Column(nullable = false)
  private int ordinal;

  protected TournamentParticipantJpaEntity() {

  }

  public TournamentParticipantJpaEntity(UUID tournamentId, UUID playerId, int ordinal) {

    this.tournamentId = tournamentId;
    this.playerId = playerId;
    this.ordinal = ordinal;
  }

  public UUID getTournamentId() {

    return tournamentId;
  }

  public void setTournamentId(UUID tournamentId) {

    this.tournamentId = tournamentId;
  }

  public UUID getPlayerId() {

    return playerId;
  }

  public void setPlayerId(UUID playerId) {

    this.playerId = playerId;
  }

  public int getOrdinal() {

    return ordinal;
  }

  public void setOrdinal(int ordinal) {

    this.ordinal = ordinal;
  }

}
