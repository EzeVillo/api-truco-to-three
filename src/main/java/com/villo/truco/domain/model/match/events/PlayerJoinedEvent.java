package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.shared.DomainEventBase;

public final class PlayerJoinedEvent extends DomainEventBase {

  public PlayerJoinedEvent() {

    super("PLAYER_JOINED");
  }

}
