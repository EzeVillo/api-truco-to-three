package com.villo.truco.domain.model.cup.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class PublicCupLobbyUnavailableException extends DomainException {

  public PublicCupLobbyUnavailableException() {

    super("Public cup is no longer accepting players");
  }

}
