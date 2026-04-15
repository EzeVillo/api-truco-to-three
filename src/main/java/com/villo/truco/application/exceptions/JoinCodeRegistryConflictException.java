package com.villo.truco.application.exceptions;

public final class JoinCodeRegistryConflictException extends ApplicationException {

  public JoinCodeRegistryConflictException() {

    super(ApplicationStatus.CONFLICT,
        "Join code is already assigned to another resource. Retry the request");
  }

}
