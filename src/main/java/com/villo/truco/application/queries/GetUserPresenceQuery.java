package com.villo.truco.application.queries;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record GetUserPresenceQuery(PlayerId requester) {

  public GetUserPresenceQuery {

    Objects.requireNonNull(requester);
  }

  public GetUserPresenceQuery(final String requester) {

    this(PlayerId.of(requester));
  }

}
