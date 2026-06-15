package com.villo.truco.application.events;

import com.villo.truco.domain.model.gameplay.valueobjects.RecordedDecision;
import java.util.Objects;

public record RecordedDecisionCaptured(RecordedDecision decision) implements
    PostCommitApplicationEvent {

  public RecordedDecisionCaptured {

    Objects.requireNonNull(decision, "decision is required");
  }

}
