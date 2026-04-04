package com.villo.truco.domain.model.league.events;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;

public final class PublicLeagueLobbyOpenedEvent extends LeagueDomainEvent {

  public PublicLeagueLobbyOpenedEvent(final LeagueId leagueId, final PlayerId creator) {

    super("PUBLIC_LEAGUE_LOBBY_OPENED", leagueId, List.of(creator));
  }

}
