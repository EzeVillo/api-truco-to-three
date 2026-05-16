package com.villo.truco.profile.domain.model;

import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record PlayerStatsSnapshot(PlayerId playerId, int matchesPlayed, int matchesWon,
                                  int matchesLost) {

}
