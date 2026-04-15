package com.villo.truco.domain.shared;

import com.villo.truco.domain.shared.valueobjects.JoinCode;
import com.villo.truco.domain.shared.valueobjects.JoinTargetType;
import java.util.Objects;
import java.util.UUID;

public record JoinCodeRegistration(JoinCode joinCode, JoinTargetType targetType, UUID targetId) {

  public JoinCodeRegistration {

    Objects.requireNonNull(joinCode, "JoinCode cannot be null");
    Objects.requireNonNull(targetType, "JoinTargetType cannot be null");
    Objects.requireNonNull(targetId, "TargetId cannot be null");
  }

}
