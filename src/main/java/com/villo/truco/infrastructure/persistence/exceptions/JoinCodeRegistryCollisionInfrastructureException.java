package com.villo.truco.infrastructure.persistence.exceptions;

import com.villo.truco.domain.shared.exceptions.RetriableException;
import com.villo.truco.domain.shared.valueobjects.JoinCode;
import com.villo.truco.domain.shared.valueobjects.JoinTargetType;
import java.util.UUID;

public final class JoinCodeRegistryCollisionInfrastructureException extends RetriableException {

  public JoinCodeRegistryCollisionInfrastructureException(final JoinCode joinCode,
      final JoinTargetType existingTargetType, final UUID existingTargetId,
      final JoinTargetType attemptedTargetType, final UUID attemptedTargetId) {

    super("Join code collision for " + joinCode.value() + ": existing mapping=" + existingTargetType
        + ":" + existingTargetId + ", attempted mapping=" + attemptedTargetType + ":"
        + attemptedTargetId);
  }

}
