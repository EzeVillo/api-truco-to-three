package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.RematchSessionDomainEventHandler;
import com.villo.truco.domain.model.rematch.events.RematchSessionClosedByLeaveEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionConfirmedEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionDomainEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionExpiredEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionOpenedEvent;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.services.FriendPresenceAvailabilityNotifier;
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
  private final FriendPresenceAvailabilityNotifier friendPresenceAvailabilityNotifier;

  public RematchPresenceEventTranslator(final PresenceNotifier presenceNotifier,
      final FriendPresenceAvailabilityNotifier friendPresenceAvailabilityNotifier) {

    this.presenceNotifier = Objects.requireNonNull(presenceNotifier);
    this.friendPresenceAvailabilityNotifier = Objects.requireNonNull(
        friendPresenceAvailabilityNotifier);
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
      this.publishAvailabilityChanges(confirmed.getNewPlayerOneId(), event);
      this.publishAvailabilityChanges(confirmed.getNewPlayerTwoId(), event);
    } else if (event instanceof RematchSessionClosedByLeaveEvent closed) {
      this.presenceNotifier.notifyPlayers(
          Arrays.asList(closed.getActorId(), closed.getOtherPlayerId()));
      this.publishAvailabilityChanges(closed.getActorId(), event);
      this.publishAvailabilityChanges(closed.getOtherPlayerId(), event);
    } else if (event instanceof RematchSessionExpiredEvent expired) {
      this.presenceNotifier.notifyPlayers(
          Arrays.asList(expired.getPlayerOneId(), expired.getPlayerTwoId()));
      this.publishAvailabilityChanges(expired.getPlayerOneId(), event);
      this.publishAvailabilityChanges(expired.getPlayerTwoId(), event);
    }
  }

  private void publishAvailabilityChanges(final PlayerId player,
      final RematchSessionDomainEvent event) {

    this.friendPresenceAvailabilityNotifier.notifyAvailabilityChanged(player, event.getTimestamp());
  }

}
