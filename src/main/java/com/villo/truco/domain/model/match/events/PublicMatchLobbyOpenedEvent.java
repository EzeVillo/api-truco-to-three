package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public final class PublicMatchLobbyOpenedEvent extends MatchDomainEvent {

  public PublicMatchLobbyOpenedEvent(final MatchId matchId, final PlayerId playerOne) {

    super("PUBLIC_MATCH_LOBBY_OPENED", matchId, playerOne, null);
  }

}
