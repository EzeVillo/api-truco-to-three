package com.villo.truco.auth.application.model;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record AuthenticatedSessionIdentity(PlayerId playerId, String username, String tokenUse) {

  public AuthenticatedSessionIdentity {

    Objects.requireNonNull(playerId);
    Objects.requireNonNull(tokenUse);
  }

}
