package com.villo.truco.auth.application.model;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record GuestAuthenticatedSession(PlayerId playerId, String accessToken,
                                        long accessTokenExpiresIn) {

  public GuestAuthenticatedSession {

    Objects.requireNonNull(playerId);
    Objects.requireNonNull(accessToken);
  }

}
