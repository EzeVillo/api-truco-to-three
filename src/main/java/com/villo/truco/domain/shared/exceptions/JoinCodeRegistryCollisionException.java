package com.villo.truco.domain.shared.exceptions;

import com.villo.truco.domain.shared.valueobjects.JoinCode;
import com.villo.truco.domain.shared.valueobjects.JoinTargetType;
import java.util.UUID;

public final class JoinCodeRegistryCollisionException extends RuntimeException {

  public JoinCodeRegistryCollisionException(final JoinCode joinCode,
      final JoinTargetType existingTargetType, final UUID existingTargetId,
      final JoinTargetType attemptedTargetType, final UUID attemptedTargetId) {

    super("Join code collision for " + joinCode.value() + ": existing mapping=" + existingTargetType
        + ":" + existingTargetId + ", attempted mapping=" + attemptedTargetType + ":"
        + attemptedTargetId);
  }

}
