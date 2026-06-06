package com.villo.truco.social.application.services;

import com.villo.truco.application.ports.TransactionalRunner;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.events.FriendAvailabilityNotification;
import java.util.List;
import java.util.Objects;

/**
 * Publica deltas de disponibilidad hacia los amigos aceptados cuando un jugador cambia su presencia
 * online (al conectar su primera sesion WebSocket o desconectar la ultima). La resolucion de
 * destinatarios y snapshot corre dentro de una transaccion para que las queries del resolver
 * dispongan de sesion, y el push STOMP se difiere a post-commit via
 * {@link FriendAvailabilityNotification}.
 */
public final class FriendPresenceAvailabilityNotifier {

  public static final String AVAILABILITY_CHANGED_EVENT_TYPE = "FRIEND_AVAILABILITY_CHANGED";

  private final FriendAvailabilityResolver friendAvailabilityResolver;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final TransactionalRunner transactionalRunner;

  public FriendPresenceAvailabilityNotifier(
      final FriendAvailabilityResolver friendAvailabilityResolver,
      final ApplicationEventPublisher applicationEventPublisher,
      final TransactionalRunner transactionalRunner) {

    this.friendAvailabilityResolver = Objects.requireNonNull(friendAvailabilityResolver);
    this.applicationEventPublisher = Objects.requireNonNull(applicationEventPublisher);
    this.transactionalRunner = Objects.requireNonNull(transactionalRunner);
  }

  public void notifyPresenceChanged(final PlayerId player) {

    this.notifyAvailabilityChanged(player, System.currentTimeMillis());
  }

  public void notifyAvailabilityChanged(final PlayerId player, final long timestamp) {

    Objects.requireNonNull(player);

    this.transactionalRunner.run(() -> {
      final var changes = this.friendAvailabilityResolver.resolveAvailabilityChangesForPlayer(
          player);
      for (final var entry : changes.entrySet()) {
        this.applicationEventPublisher.publish(
            new FriendAvailabilityNotification(List.of(entry.getKey()),
                AVAILABILITY_CHANGED_EVENT_TYPE, timestamp, entry.getValue().toPayload()));
      }
    });
  }

}
