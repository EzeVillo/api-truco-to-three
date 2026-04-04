package com.villo.truco.domain.model.league.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class PublicLeagueLobbyUnavailableException extends DomainException {

  public PublicLeagueLobbyUnavailableException() {

    super("Public league is no longer accepting players");
  }

}
