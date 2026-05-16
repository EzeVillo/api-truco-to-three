package com.villo.truco.profile.domain.model;

import com.villo.truco.domain.shared.AggregateBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class PlayerStats extends AggregateBase<PlayerId> {

  private int matchesPlayed;
  private int matchesWon;
  private int matchesLost;

  private PlayerStats(final PlayerId playerId, final int matchesPlayed, final int matchesWon,
      final int matchesLost) {

    super(Objects.requireNonNull(playerId));
    this.matchesPlayed = matchesPlayed;
    this.matchesWon = matchesWon;
    this.matchesLost = matchesLost;
  }

  public static PlayerStats create(final PlayerId playerId) {

    return new PlayerStats(playerId, 0, 0, 0);
  }

  static PlayerStats reconstruct(final PlayerId playerId, final int matchesPlayed,
      final int matchesWon, final int matchesLost) {

    return new PlayerStats(playerId, matchesPlayed, matchesWon, matchesLost);
  }

  public void recordOutcome(final MatchOutcome outcome) {

    this.matchesPlayed++;
    if (outcome == MatchOutcome.WON) {
      this.matchesWon++;
    } else {
      this.matchesLost++;
    }
  }

  public int matchesPlayed() {

    return this.matchesPlayed;
  }

  public int matchesWon() {

    return this.matchesWon;
  }

  public int matchesLost() {

    return this.matchesLost;
  }

  public int winRate() {

    if (this.matchesPlayed == 0) {
      return 0;
    }
    return (int) Math.round((double) this.matchesWon / this.matchesPlayed * 100);
  }

  public PlayerStatsSnapshot snapshot() {

    return new PlayerStatsSnapshot(this.id, this.matchesPlayed, this.matchesWon, this.matchesLost);
  }

}
