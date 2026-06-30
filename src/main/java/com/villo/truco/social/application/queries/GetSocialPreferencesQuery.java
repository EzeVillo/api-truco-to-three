package com.villo.truco.social.application.queries;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record GetSocialPreferencesQuery(PlayerId playerId) {

  public GetSocialPreferencesQuery {

    Objects.requireNonNull(playerId);
  }

  public GetSocialPreferencesQuery(final String playerId) {

    this(PlayerId.of(playerId));
  }

}
