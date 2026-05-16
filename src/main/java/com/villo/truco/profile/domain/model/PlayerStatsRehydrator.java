package com.villo.truco.profile.domain.model;

public final class PlayerStatsRehydrator {

  private PlayerStatsRehydrator() {

  }

  public static PlayerStats rehydrate(final PlayerStatsSnapshot snapshot) {

    return PlayerStats.reconstruct(snapshot.playerId(), snapshot.matchesPlayed(),
        snapshot.matchesWon(), snapshot.matchesLost());
  }

}
