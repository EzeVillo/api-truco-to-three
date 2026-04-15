package com.villo.truco.social.application.queries;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record GetSentFriendshipRequestsQuery(PlayerId requesterId) {

  public GetSentFriendshipRequestsQuery {

    Objects.requireNonNull(requesterId);
  }

  public GetSentFriendshipRequestsQuery(final String requesterId) {

    this(PlayerId.of(requesterId));
  }

}
