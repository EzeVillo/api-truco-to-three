package com.villo.truco.domain.model.gameplay.valueobjects;

import com.villo.truco.domain.model.match.MatchSnapshot;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import java.time.Instant;
import java.util.Objects;

public record RecordedDecision(MatchId matchId, long stateVersion, int gameNumber, int roundNumber,
                               ActorSeat actorSeat, ActorType actorType, RecordedAction action,
                               MatchSnapshot snapshotBefore, MatchSnapshot snapshotAfter,
                               DecisionContext context, Instant occurredAt, int schemaVersion) {

  public RecordedDecision {

    Objects.requireNonNull(matchId, "matchId is required");
    Objects.requireNonNull(actorSeat, "actorSeat is required");
    Objects.requireNonNull(actorType, "actorType is required");
    Objects.requireNonNull(action, "action is required");
    Objects.requireNonNull(snapshotBefore, "snapshotBefore is required");
    Objects.requireNonNull(snapshotAfter, "snapshotAfter is required");
    Objects.requireNonNull(context, "context is required");
    Objects.requireNonNull(occurredAt, "occurredAt is required");
  }

}
