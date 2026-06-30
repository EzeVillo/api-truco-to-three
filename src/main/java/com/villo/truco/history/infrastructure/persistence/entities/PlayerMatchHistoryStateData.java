package com.villo.truco.history.infrastructure.persistence.entities;

import java.util.List;
import java.util.UUID;

public record PlayerMatchHistoryStateData(List<EntryData> entries) {

  public record EntryData(UUID matchId, UUID opponentId, String outcome, String endReason,
                          int ownGamesWon, int opponentGamesWon, long endedAtEpochMilli) {

  }

}
