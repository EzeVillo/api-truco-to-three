package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.CupDomainEventHandler;
import com.villo.truco.domain.model.cup.events.CupAdvancedEvent;
import com.villo.truco.domain.model.cup.events.CupCancelledEvent;
import com.villo.truco.domain.model.cup.events.CupCreatedEvent;
import com.villo.truco.domain.model.cup.events.CupDomainEvent;
import com.villo.truco.domain.model.cup.events.CupFinishedEvent;
import com.villo.truco.domain.model.cup.events.CupMatchActivatedEvent;
import com.villo.truco.domain.model.cup.events.CupPlayerForfeitedEvent;
import com.villo.truco.domain.model.cup.events.CupPlayerJoinedEvent;
import com.villo.truco.domain.model.cup.events.CupPlayerLeftEvent;
import com.villo.truco.domain.model.cup.events.CupStartedEvent;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Proyecta las transiciones de ocupacion de copa como notificaciones de presencia: alta de un
 * jugador, arranque/avance del torneo (que cambia la partida actual) y liberacion (finalizada,
 * cancelada, salida o rendicion). Notifica a los participantes del evento mas el jugador especifico
 * de los eventos de salida/rendicion.
 */
public final class CupPresenceEventTranslator implements CupDomainEventHandler<CupDomainEvent> {

  private final PresenceNotifier presenceNotifier;

  public CupPresenceEventTranslator(final PresenceNotifier presenceNotifier) {

    this.presenceNotifier = Objects.requireNonNull(presenceNotifier);
  }

  private static boolean isOccupancyTransition(final CupDomainEvent event) {

    return event instanceof CupCreatedEvent || event instanceof CupPlayerJoinedEvent
        || event instanceof CupStartedEvent || event instanceof CupMatchActivatedEvent
        || event instanceof CupAdvancedEvent || event instanceof CupFinishedEvent
        || event instanceof CupCancelledEvent || event instanceof CupPlayerLeftEvent
        || event instanceof CupPlayerForfeitedEvent;
  }

  @Override
  public Class<CupDomainEvent> eventType() {

    return CupDomainEvent.class;
  }

  @Override
  public void handle(final CupDomainEvent event) {

    if (!isOccupancyTransition(event)) {
      return;
    }

    final Set<PlayerId> affected = new LinkedHashSet<>(event.getParticipants());
    if (event instanceof CupPlayerLeftEvent left) {
      affected.add(left.getPlayerId());
    } else if (event instanceof CupPlayerForfeitedEvent forfeited) {
      affected.add(forfeited.getForfeiter());
    }

    this.presenceNotifier.notifyPlayers(affected);
  }

}
