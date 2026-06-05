package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.LeagueDomainEventHandler;
import com.villo.truco.domain.model.league.events.LeagueAdvancedEvent;
import com.villo.truco.domain.model.league.events.LeagueCancelledEvent;
import com.villo.truco.domain.model.league.events.LeagueDomainEvent;
import com.villo.truco.domain.model.league.events.LeagueFinishedEvent;
import com.villo.truco.domain.model.league.events.LeagueMatchActivatedEvent;
import com.villo.truco.domain.model.league.events.LeaguePlayerForfeitedEvent;
import com.villo.truco.domain.model.league.events.LeaguePlayerJoinedEvent;
import com.villo.truco.domain.model.league.events.LeaguePlayerLeftEvent;
import com.villo.truco.domain.model.league.events.LeagueStartedEvent;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Proyecta las transiciones de ocupacion de liga como notificaciones de presencia: alta de un
 * jugador, arranque/avance del torneo (que cambia la partida actual) y liberacion (finalizada,
 * cancelada, salida o rendicion). Notifica a los participantes del evento mas el jugador especifico
 * de los eventos de salida/rendicion (que ya no figura en la lista de participantes).
 */
public final class LeaguePresenceEventTranslator implements
    LeagueDomainEventHandler<LeagueDomainEvent> {

  private final PresenceNotifier presenceNotifier;

  public LeaguePresenceEventTranslator(final PresenceNotifier presenceNotifier) {

    this.presenceNotifier = Objects.requireNonNull(presenceNotifier);
  }

  private static boolean isOccupancyTransition(final LeagueDomainEvent event) {

    return event instanceof LeaguePlayerJoinedEvent || event instanceof LeagueStartedEvent
        || event instanceof LeagueMatchActivatedEvent || event instanceof LeagueAdvancedEvent
        || event instanceof LeagueFinishedEvent || event instanceof LeagueCancelledEvent
        || event instanceof LeaguePlayerLeftEvent || event instanceof LeaguePlayerForfeitedEvent;
  }

  @Override
  public Class<LeagueDomainEvent> eventType() {

    return LeagueDomainEvent.class;
  }

  @Override
  public void handle(final LeagueDomainEvent event) {

    if (!isOccupancyTransition(event)) {
      return;
    }

    final Set<PlayerId> affected = new LinkedHashSet<>(event.getParticipants());
    if (event instanceof LeaguePlayerLeftEvent left) {
      affected.add(left.getPlayerId());
    } else if (event instanceof LeaguePlayerForfeitedEvent forfeited) {
      affected.add(forfeited.getForfeiter());
    }

    this.presenceNotifier.notifyPlayers(affected);
  }

}
