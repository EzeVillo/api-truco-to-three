package com.villo.truco.domain.model.league.events;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;

/**
 * Emitido cuando un jugador crea una liga y queda como host a la espera de participantes. Marca que
 * el creador pasa a estar ocupado (no puede entrar a otro recurso) ya en estado de espera.
 */
public final class LeagueCreatedEvent extends LeagueDomainEvent {

  public LeagueCreatedEvent(final LeagueId leagueId, final List<PlayerId> participants) {

    super("LEAGUE_CREATED", leagueId, participants);
  }

}
