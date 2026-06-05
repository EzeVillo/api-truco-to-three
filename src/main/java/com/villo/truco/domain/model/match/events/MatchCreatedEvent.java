package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

/**
 * Emitido cuando un jugador crea una partida y queda como host a la espera de rival. Marca que el
 * creador pasa a estar ocupado (no puede entrar a otro recurso) ya en estado de espera, aunque
 * todavia no se haya unido nadie ni arrancado el juego.
 */
public final class MatchCreatedEvent extends MatchDomainEvent {

  public MatchCreatedEvent(final MatchId matchId, final PlayerId creator) {

    super("MATCH_CREATED", matchId, creator, null);
  }

}
