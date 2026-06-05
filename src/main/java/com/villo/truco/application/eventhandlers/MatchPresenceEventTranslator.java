package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.domain.model.match.events.GameStartedEvent;
import com.villo.truco.domain.model.match.events.MatchAbandonedEvent;
import com.villo.truco.domain.model.match.events.MatchCancelledEvent;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
import com.villo.truco.domain.model.match.events.MatchPlayerLeftEvent;
import com.villo.truco.domain.model.match.events.PlayerJoinedEvent;
import java.util.Arrays;
import java.util.Objects;

/**
 * Proyecta las transiciones de ocupacion de partida como notificaciones de presencia. Cubre el
 * ingreso a una partida cuando un segundo jugador se une ({@link PlayerJoinedEvent}) y cuando la
 * partida arranca su primer juego ({@link GameStartedEvent} con {@code gameNumber == 1}), que es el
 * unico evento de ocupacion en los flujos sin "join" como el bot match y el quick match (el creador
 * nunca dispara un PlayerJoinedEvent). Tambien cubre la liberacion (finalizada, cancelada,
 * abandonada, rendida o por salida de un jugador). Las partidas de torneo se cubren ademas via los
 * traductores de liga/copa, que re-resuelven y encuentran la partida activa del participante.
 */
public final class MatchPresenceEventTranslator implements
    MatchDomainEventHandler<MatchDomainEvent> {

  private final PresenceNotifier presenceNotifier;

  public MatchPresenceEventTranslator(final PresenceNotifier presenceNotifier) {

    this.presenceNotifier = Objects.requireNonNull(presenceNotifier);
  }

  private static boolean isOccupancyTransition(final MatchDomainEvent event) {

    return event instanceof PlayerJoinedEvent || (event instanceof GameStartedEvent gameStarted
        && gameStarted.getGameNumber() == 1) || event instanceof MatchFinishedEvent
        || event instanceof MatchCancelledEvent || event instanceof MatchAbandonedEvent
        || event instanceof MatchForfeitedEvent || event instanceof MatchPlayerLeftEvent;
  }

  @Override
  public Class<MatchDomainEvent> eventType() {

    return MatchDomainEvent.class;
  }

  @Override
  public void handle(final MatchDomainEvent event) {

    if (!isOccupancyTransition(event)) {
      return;
    }

    this.presenceNotifier.notifyPlayers(Arrays.asList(event.getPlayerOne(), event.getPlayerTwo()));
  }

}
