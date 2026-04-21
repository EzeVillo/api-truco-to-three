package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.JoinByCodeCommand;
import com.villo.truco.application.dto.JoinResourceDTO;
import com.villo.truco.application.ports.in.JoinByCodeUseCase;
import java.util.Objects;

public final class JoinByCodeCommandHandler implements JoinByCodeUseCase {

  private final JoinTargetDispatcher joinTargetDispatcher;

  public JoinByCodeCommandHandler(final JoinTargetDispatcher joinTargetDispatcher) {

    this.joinTargetDispatcher = Objects.requireNonNull(joinTargetDispatcher);
  }

  @Override
  public JoinResourceDTO handle(final JoinByCodeCommand command) {

    return this.joinTargetDispatcher.joinByCode(command.playerId(), command.joinCode());
  }

}
