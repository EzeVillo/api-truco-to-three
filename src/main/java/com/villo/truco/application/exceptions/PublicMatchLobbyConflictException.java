package com.villo.truco.application.exceptions;

public final class PublicMatchLobbyConflictException extends ApplicationException {

  public PublicMatchLobbyConflictException() {

    super(ApplicationStatus.CONFLICT,
        "Another request occupied the last available slot in this public match");
  }

}
