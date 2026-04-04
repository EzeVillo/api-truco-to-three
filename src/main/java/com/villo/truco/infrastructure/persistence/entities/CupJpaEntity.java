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
@Table(name = "cups")
public class CupJpaEntity {

  @OneToMany(mappedBy = "cup", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("ordinal ASC")
  private final List<CupParticipantJpaEntity> participants = new ArrayList<>();
  @OneToMany(mappedBy = "cup", cascade = CascadeType.ALL, orphanRemoval = true)
  private final List<CupBoutJpaEntity> bouts = new ArrayList<>();
  @OneToMany(mappedBy = "cup", cascade = CascadeType.ALL, orphanRemoval = true)
  private final List<CupForfeitedPlayerJpaEntity> forfeitedPlayers = new ArrayList<>();
  @Id
  private UUID id;
  @Column(name = "number_of_players", nullable = false)
  private int numberOfPlayers;
  @Column(name = "games_to_play", nullable = false)
  private int gamesToPlay;
  @Column(name = "invite_code")
  private String inviteCode;
  @Column(nullable = false)
  private String visibility;
  @Column(nullable = false)
  private String status;
  @Column
  private UUID champion;
  @Column(name = "last_activity_at", nullable = false)
  private Instant lastActivityAt;
  @Version
  private int version;

  public CupJpaEntity() {

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

  public String getInviteCode() {

    return inviteCode;
  }

  public void setInviteCode(String inviteCode) {

    this.inviteCode = inviteCode;
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

  public UUID getChampion() {

    return champion;
  }

  public void setChampion(UUID champion) {

    this.champion = champion;
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

  public List<CupParticipantJpaEntity> getParticipants() {

    return participants;
  }

  public void addParticipant(CupParticipantJpaEntity p) {

    participants.add(p);
    p.setCup(this);
  }

  public List<CupBoutJpaEntity> getBouts() {

    return bouts;
  }

  public void addBout(CupBoutJpaEntity b) {

    bouts.add(b);
    b.setCup(this);
  }

  public List<CupForfeitedPlayerJpaEntity> getForfeitedPlayers() {

    return forfeitedPlayers;
  }

  public void addForfeitedPlayer(CupForfeitedPlayerJpaEntity fp) {

    forfeitedPlayers.add(fp);
    fp.setCup(this);
  }

}
