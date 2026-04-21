package com.villo.truco.social.application.queries;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record GetFriendshipRequestsQuery(PlayerId playerId) {

  public GetFriendshipRequestsQuery {

    Objects.requireNonNull(playerId);
  }

  public GetFriendshipRequestsQuery(final String playerId) {

    this(PlayerId.of(playerId));
  }

}
