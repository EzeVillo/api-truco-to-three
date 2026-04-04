package com.villo.truco.domain.model.match.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class PublicMatchLobbyUnavailableException extends DomainException {

  public PublicMatchLobbyUnavailableException() {

    super("Public match is no longer accepting players");
  }

}
