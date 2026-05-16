package com.villo.truco.profile.application.exceptions;

import com.villo.truco.application.exceptions.ApplicationException;
import com.villo.truco.application.exceptions.ApplicationStatus;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public final class PlayerNotFoundException extends ApplicationException {

  public PlayerNotFoundException(final PlayerId playerId) {

    super(ApplicationStatus.NOT_FOUND, "Player not found: " + playerId);
  }

  public PlayerNotFoundException(final String username) {

    super(ApplicationStatus.NOT_FOUND, "Player not found: " + username);
  }

}
