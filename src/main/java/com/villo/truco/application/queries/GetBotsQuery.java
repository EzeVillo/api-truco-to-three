package com.villo.truco.application.queries;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record GetBotsQuery(PlayerId playerId) {

  public GetBotsQuery {

    Objects.requireNonNull(playerId, "playerId cannot be null");
  }

}
