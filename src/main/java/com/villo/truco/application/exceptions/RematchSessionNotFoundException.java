package com.villo.truco.application.exceptions;

public final class RematchSessionNotFoundException extends ApplicationException {

  public RematchSessionNotFoundException() {

    super(ApplicationStatus.NOT_FOUND, "Rematch session not found");
  }

}
