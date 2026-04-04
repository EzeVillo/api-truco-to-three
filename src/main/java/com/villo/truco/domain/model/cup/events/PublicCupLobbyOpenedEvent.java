package com.villo.truco.domain.model.cup.events;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;

public final class PublicCupLobbyOpenedEvent extends CupDomainEvent {

  public PublicCupLobbyOpenedEvent(final CupId cupId, final PlayerId creator) {

    super("PUBLIC_CUP_LOBBY_OPENED", cupId, List.of(creator));
  }

}
