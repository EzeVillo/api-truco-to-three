package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.StopSpectatingMatchCommand;
import com.villo.truco.application.ports.in.StopSpectatingMatchUseCase;
import java.util.Objects;

public final class StopSpectatingMatchCommandHandler implements StopSpectatingMatchUseCase {

  private final SpectatorshipLifecycleManager lifecycleManager;

  public StopSpectatingMatchCommandHandler(final SpectatorshipLifecycleManager lifecycleManager) {

    this.lifecycleManager = Objects.requireNonNull(lifecycleManager);
  }

  @Override
  public Void handle(final StopSpectatingMatchCommand command) {

    this.lifecycleManager.stopManually(command.spectatorId());
    return null;
  }

}
