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
@Table(name = "leagues")
public class LeagueJpaEntity {

  @Id
  private UUID id;

  @Column(name = "number_of_players", nullable = false)
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
  @JoinColumn(name = "league_id")
  @OrderBy("ordinal ASC")
  private List<LeagueParticipantJpaEntity> participants = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "league_id")
  private List<LeagueFixtureJpaEntity> fixtures = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "league_id")
  private List<LeagueWinJpaEntity> wins = new ArrayList<>();

  public LeagueJpaEntity() {

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

  public List<LeagueParticipantJpaEntity> getParticipants() {

    return participants;
  }

  public void setParticipants(List<LeagueParticipantJpaEntity> participants) {

    this.participants = participants;
  }

  public List<LeagueFixtureJpaEntity> getFixtures() {

    return fixtures;
  }

  public void setFixtures(List<LeagueFixtureJpaEntity> fixtures) {

    this.fixtures = fixtures;
  }

  public List<LeagueWinJpaEntity> getWins() {

    return wins;
  }

  public void setWins(List<LeagueWinJpaEntity> wins) {

    this.wins = wins;
  }

}
