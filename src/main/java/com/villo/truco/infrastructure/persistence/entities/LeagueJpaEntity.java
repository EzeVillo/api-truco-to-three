package com.villo.truco.infrastructure.persistence.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "leagues")
public class LeagueJpaEntity {

  @OneToMany(mappedBy = "league", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("ordinal ASC")
  private final List<LeagueParticipantJpaEntity> participants = new ArrayList<>();
  @OneToMany(mappedBy = "league", cascade = CascadeType.ALL, orphanRemoval = true)
  private final List<LeagueFixtureJpaEntity> fixtures = new ArrayList<>();
  @OneToMany(mappedBy = "league", cascade = CascadeType.ALL, orphanRemoval = true)
  private final List<LeagueWinJpaEntity> wins = new ArrayList<>();
  @Id
  private UUID id;
  @Column(name = "number_of_players", nullable = false)
  private int numberOfPlayers;
  @Column(name = "games_to_play", nullable = false)
  private int gamesToPlay;
  @Column(name = "join_code", nullable = false)
  private String joinCode;
  @Column(nullable = false)
  private String visibility;
  @Column(nullable = false)
  private String status;
  @Column(name = "last_activity_at", nullable = false)
  private Instant lastActivityAt;
  @Version
  private int version;

  public LeagueJpaEntity() {

  }

  @PrePersist
  void onPrePersist() {

    this.lastActivityAt = Instant.now();
  }

  @PreUpdate
  void onPreUpdate() {

    this.lastActivityAt = Instant.now();
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

  public String getJoinCode() {

    return joinCode;
  }

  public void setJoinCode(String joinCode) {

    this.joinCode = joinCode;
  }

  public String getVisibility() {

    return visibility;
  }

  public void setVisibility(String visibility) {

    this.visibility = visibility;
  }

  public String getStatus() {

    return status;
  }

  public void setStatus(String status) {

    this.status = status;
  }

  public Instant getLastActivityAt() {

    return lastActivityAt;
  }

  public void setLastActivityAt(Instant lastActivityAt) {

    this.lastActivityAt = lastActivityAt;
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

  public void addParticipant(LeagueParticipantJpaEntity p) {

    participants.add(p);
    p.setLeague(this);
  }

  public List<LeagueFixtureJpaEntity> getFixtures() {

    return fixtures;
  }

  public void addFixture(LeagueFixtureJpaEntity f) {

    fixtures.add(f);
    f.setLeague(this);
  }

  public List<LeagueWinJpaEntity> getWins() {

    return wins;
  }

  public void addWin(LeagueWinJpaEntity w) {

    wins.add(w);
    w.setLeague(this);
  }

}
