package com.villo.truco.infrastructure.persistence.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tournaments")
public class TournamentJpaEntity {

  @Id
  private UUID id;

  @Column(name = "capacity", nullable = false)
  private int numberOfPlayers;

  @Column(name = "games_to_play", nullable = false)
  private int gamesToPlay;

  @Column(name = "invite_code", nullable = false)
  private String inviteCode;

  @Column(nullable = false)
  private String status;

  @Version
  private int version;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "tournament_id")
  @OrderBy("ordinal ASC")
  private List<TournamentParticipantJpaEntity> participants = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "tournament_id")
  private List<TournamentFixtureJpaEntity> fixtures = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "tournament_id")
  private List<TournamentWinJpaEntity> wins = new ArrayList<>();

  public TournamentJpaEntity() {

  }

  public UUID getId() {

    return id;
  }

  public void setId(UUID id) {

    this.id = id;
  }

  public int getNumberOfPlayers() {

    return numberOfPlayers;
  }

  public void setNumberOfPlayers(int numberOfPlayers) {

    this.numberOfPlayers = numberOfPlayers;
  }

  public int getGamesToPlay() {

    return gamesToPlay;
  }

  public void setGamesToPlay(int gamesToPlay) {

    this.gamesToPlay = gamesToPlay;
  }

  public String getInviteCode() {

    return inviteCode;
  }

  public void setInviteCode(String inviteCode) {

    this.inviteCode = inviteCode;
  }

  public String getStatus() {

    return status;
  }

  public void setStatus(String status) {

    this.status = status;
  }

  public int getVersion() {

    return version;
  }

  public void setVersion(int version) {

    this.version = version;
  }

  public List<TournamentParticipantJpaEntity> getParticipants() {

    return participants;
  }

  public void setParticipants(List<TournamentParticipantJpaEntity> participants) {

    this.participants = participants;
  }

  public List<TournamentFixtureJpaEntity> getFixtures() {

    return fixtures;
  }

  public void setFixtures(List<TournamentFixtureJpaEntity> fixtures) {

    this.fixtures = fixtures;
  }

  public List<TournamentWinJpaEntity> getWins() {

    return wins;
  }

  public void setWins(List<TournamentWinJpaEntity> wins) {

    this.wins = wins;
  }

}
