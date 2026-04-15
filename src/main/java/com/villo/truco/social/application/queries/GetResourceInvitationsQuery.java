package com.villo.truco.social.application.queries;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record GetResourceInvitationsQuery(PlayerId playerId) {

  public GetResourceInvitationsQuery {

    Objects.requireNonNull(playerId);
  }

  public GetResourceInvitationsQuery(final String playerId) {

    this(PlayerId.of(playerId));
  }

}
