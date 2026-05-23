package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.CancelQuickMatchSearchCommand;
import com.villo.truco.application.ports.in.CancelQuickMatchSearchUseCase;
import com.villo.truco.domain.ports.QuickMatchQueuePort;
import java.util.Objects;

public final class CancelQuickMatchSearchCommandHandler implements CancelQuickMatchSearchUseCase {

  private final QuickMatchQueuePort queuePort;

  public CancelQuickMatchSearchCommandHandler(final QuickMatchQueuePort queuePort) {

    this.queuePort = Objects.requireNonNull(queuePort);
  }

  @Override
  public Void handle(final CancelQuickMatchSearchCommand command) {

    this.queuePort.tryDequeue(command.playerId());
    return null;
  }

}
