package com.villo.truco.domain.model.gameplay.valueobjects;

import java.util.Objects;

public record RecordedAction(RecordedActionType type, Object detail) {

  public RecordedAction {

    Objects.requireNonNull(type, "type is required");
  }

}
