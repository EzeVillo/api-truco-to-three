package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.RematchSessionDomainEventHandler;
import com.villo.truco.domain.model.rematch.events.RematchSessionClosedByLeaveEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionConfirmedEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionDomainEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionExpiredEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionOpenedEvent;
import java.util.Arrays;
import java.util.Objects;

/**
 * Proyecta las transiciones de ocupacion de revancha como notificaciones de presencia: apertura de
 * la sesion ({@link RematchSessionOpenedEvent}) y su cierre (confirmada, cerrada por salida o
 * expirada). En todos los casos el evento expone ambos jugadores de la sesion.
 */
public final class RematchPresenceEventTranslator implements
    RematchSessionDomainEventHandler<RematchSessionDomainEvent> {

  private final PresenceNotifier presenceNotifier;

  public RematchPresenceEventTranslator(final PresenceNotifier presenceNotifier) {

    this.presenceNotifier = Objects.requireNonNull(presenceNotifier);
  }

  @Override
  public Class<RematchSessionDomainEvent> eventType() {

    return RematchSessionDomainEvent.class;
  }

  @Override
  public void handle(final RematchSessionDomainEvent event) {

    if (event instanceof RematchSessionOpenedEvent opened) {
      this.presenceNotifier.notifyPlayers(
          Arrays.asList(opened.getPlayerOneId(), opened.getPlayerTwoId()));
    } else if (event instanceof RematchSessionConfirmedEvent confirmed) {
      this.presenceNotifier.notifyPlayers(
          Arrays.asList(confirmed.getNewPlayerOneId(), confirmed.getNewPlayerTwoId()));
    } else if (event instanceof RematchSessionClosedByLeaveEvent closed) {
      this.presenceNotifier.notifyPlayers(
          Arrays.asList(closed.getActorId(), closed.getOtherPlayerId()));
    } else if (event instanceof RematchSessionExpiredEvent expired) {
      this.presenceNotifier.notifyPlayers(
          Arrays.asList(expired.getPlayerOneId(), expired.getPlayerTwoId()));
    }
  }

}
