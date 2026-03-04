package com.villo.truco.infrastructure.websocket;

import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;

public final class WebSocketUserNaming {

  private WebSocketUserNaming() {

  }

  public static String userName(final MatchId matchId, final PlayerId playerId) {

    return matchId.value() + "__" + playerId.value();
  }

}