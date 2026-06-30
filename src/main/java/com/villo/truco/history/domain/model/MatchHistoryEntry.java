package com.villo.truco.history.domain.model;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.Objects;

public record MatchHistoryEntry(MatchId matchId, PlayerId opponentId, MatchOutcome outcome,
                                MatchEndReason endReason, int ownGamesWon, int opponentGamesWon,
                                Instant endedAt) {

  public MatchHistoryEntry {

    Objects.requireNonNull(matchId, "matchId cannot be null");
    Objects.requireNonNull(opponentId, "opponentId cannot be null");
    Objects.requireNonNull(outcome, "outcome cannot be null");
    Objects.requireNonNull(endReason, "endReason cannot be null");
    Objects.requireNonNull(endedAt, "endedAt cannot be null");
  }

}
