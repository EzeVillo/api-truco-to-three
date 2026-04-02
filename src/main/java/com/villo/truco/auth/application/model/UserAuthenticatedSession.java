package com.villo.truco.auth.application.model;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record UserAuthenticatedSession(PlayerId playerId, String accessToken,
                                       long accessTokenExpiresIn, String refreshToken,
                                       long refreshTokenExpiresIn) {

  public UserAuthenticatedSession {

    Objects.requireNonNull(playerId);
    Objects.requireNonNull(accessToken);
    Objects.requireNonNull(refreshToken);
  }

}
