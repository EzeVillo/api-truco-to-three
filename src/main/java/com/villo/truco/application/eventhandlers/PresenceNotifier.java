package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.events.PresenceEventNotification;
import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.application.usecases.queries.UserPresenceResolver;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;

/**
 * Colaborador compartido por los traductores de presencia: dado un conjunto de jugadores afectados,
 * filtra nulos y bots, deduplica, re-resuelve el snapshot de cada jugador humano y publica una
 * {@link PresenceEventNotification} por destinatario.
 */
public final class PresenceNotifier {

  private final UserPresenceResolver userPresenceResolver;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final BotRegistry botRegistry;

  public PresenceNotifier(final UserPresenceResolver userPresenceResolver,
      final ApplicationEventPublisher applicationEventPublisher, final BotRegistry botRegistry) {

    this.userPresenceResolver = Objects.requireNonNull(userPresenceResolver);
    this.applicationEventPublisher = Objects.requireNonNull(applicationEventPublisher);
    this.botRegistry = Objects.requireNonNull(botRegistry);
  }

  public void notifyPlayers(final Collection<PlayerId> players) {

    final var humans = new LinkedHashSet<PlayerId>();
    for (final var player : players) {
      if (player != null && !this.botRegistry.isBot(player)) {
        humans.add(player);
      }
    }

    for (final var player : humans) {
      this.applicationEventPublisher.publish(
          new PresenceEventNotification(player, PresenceEventNotification.EVENT_TYPE,
              System.currentTimeMillis(), this.userPresenceResolver.resolve(player)));
    }
  }

}
