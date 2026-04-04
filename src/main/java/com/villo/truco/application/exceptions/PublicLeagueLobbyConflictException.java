package com.villo.truco.application.exceptions;

public final class PublicLeagueLobbyConflictException extends ApplicationException {

  public PublicLeagueLobbyConflictException() {

    super(ApplicationStatus.CONFLICT,
        "Another request occupied the last available slot in this public league");
  }

}
