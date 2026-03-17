package com.villo.truco.infrastructure.websocket;

import com.villo.truco.domain.shared.valueobjects.PlayerId;

public final class WebSocketUserNaming {

  private WebSocketUserNaming() {

  }

  public static String userName(final PlayerId playerId) {

    return playerId.value().toString();
  }

}
