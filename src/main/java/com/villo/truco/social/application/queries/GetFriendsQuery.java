package com.villo.truco.social.application.queries;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record GetFriendsQuery(PlayerId playerId) {

  public GetFriendsQuery {

    Objects.requireNonNull(playerId);
  }

  public GetFriendsQuery(final String playerId) {

    this(PlayerId.of(playerId));
  }

}
