package com.villo.truco.domain.model.spectator.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class SpectateNotAllowedException extends DomainException {

  public SpectateNotAllowedException() {

    super("You can only spectate matches within your own league or cup");
  }

}
