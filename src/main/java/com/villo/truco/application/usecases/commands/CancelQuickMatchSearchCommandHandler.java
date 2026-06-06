package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.CancelQuickMatchSearchCommand;
import com.villo.truco.application.eventhandlers.PresenceNotifier;
import com.villo.truco.application.ports.in.CancelQuickMatchSearchUseCase;
import com.villo.truco.domain.model.quickmatch.QuickMatchTicket;
import com.villo.truco.domain.ports.QuickMatchQueuePort;
import com.villo.truco.social.application.services.FriendAvailabilityChangeNotifier;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class CancelQuickMatchSearchCommandHandler implements CancelQuickMatchSearchUseCase {

  private final QuickMatchQueuePort queuePort;
  private final FriendAvailabilityChangeNotifier friendAvailabilityChangeNotifier;
  private final PresenceNotifier presenceNotifier;

  public CancelQuickMatchSearchCommandHandler(final QuickMatchQueuePort queuePort,
      final FriendAvailabilityChangeNotifier friendAvailabilityChangeNotifier,
      final PresenceNotifier presenceNotifier) {

    this.queuePort = Objects.requireNonNull(queuePort);
    this.friendAvailabilityChangeNotifier = Objects.requireNonNull(
        friendAvailabilityChangeNotifier);
    this.presenceNotifier = Objects.requireNonNull(presenceNotifier);
  }

  @Override
  public Void handle(final CancelQuickMatchSearchCommand command) {

    final var ticket = command.webSocketSessionId() != null ? this.tryDequeueBySession(command)
        : this.queuePort.tryDequeue(command.playerId());

    ticket.ifPresent(removed -> {
      this.friendAvailabilityChangeNotifier.notifyAvailabilityChanged(removed.playerId(),
          System.currentTimeMillis());
      this.presenceNotifier.notifyPlayers(List.of(removed.playerId()));
    });
    return null;
  }

  private Optional<QuickMatchTicket> tryDequeueBySession(
      final CancelQuickMatchSearchCommand command) {

    final var removedBySession = this.queuePort.tryDequeueBySessionId(command.webSocketSessionId());
    if (removedBySession.isPresent()) {
      return removedBySession;
    }

    return this.queuePort.findByPlayer(command.playerId())
        .filter(ticket -> ticket.webSocketSessionId() == null)
        .flatMap(ticket -> this.queuePort.tryDequeue(command.playerId()));
  }

}
