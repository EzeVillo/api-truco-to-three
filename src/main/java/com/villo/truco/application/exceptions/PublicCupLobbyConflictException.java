package com.villo.truco.application.exceptions;

public final class PublicCupLobbyConflictException extends ApplicationException {

  public PublicCupLobbyConflictException() {

    super(ApplicationStatus.CONFLICT,
        "Another request occupied the last available slot in this public cup");
  }

}
