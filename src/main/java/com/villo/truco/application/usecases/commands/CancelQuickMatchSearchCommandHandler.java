package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.CancelQuickMatchSearchCommand;
import com.villo.truco.application.ports.in.CancelQuickMatchSearchUseCase;
import com.villo.truco.domain.ports.QuickMatchQueuePort;
import com.villo.truco.social.application.services.FriendPresenceAvailabilityNotifier;
import java.util.Objects;

public final class CancelQuickMatchSearchCommandHandler implements CancelQuickMatchSearchUseCase {

  private final QuickMatchQueuePort queuePort;
  private final FriendPresenceAvailabilityNotifier friendPresenceAvailabilityNotifier;

  public CancelQuickMatchSearchCommandHandler(final QuickMatchQueuePort queuePort,
      final FriendPresenceAvailabilityNotifier friendPresenceAvailabilityNotifier) {

    this.queuePort = Objects.requireNonNull(queuePort);
    this.friendPresenceAvailabilityNotifier = Objects.requireNonNull(
        friendPresenceAvailabilityNotifier);
  }

  @Override
  public Void handle(final CancelQuickMatchSearchCommand command) {

    this.queuePort.tryDequeue(command.playerId()).ifPresent(
        ticket -> this.friendPresenceAvailabilityNotifier.notifyAvailabilityChanged(
            command.playerId(), System.currentTimeMillis()));
    return null;
  }

}
