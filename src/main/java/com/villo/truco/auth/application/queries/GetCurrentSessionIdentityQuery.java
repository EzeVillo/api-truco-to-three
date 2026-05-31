package com.villo.truco.auth.application.queries;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record GetCurrentSessionIdentityQuery(PlayerId playerId, String tokenUse) {

  public GetCurrentSessionIdentityQuery {

    Objects.requireNonNull(playerId);
    Objects.requireNonNull(tokenUse);
  }

}
