package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.RegisterTournamentMatchResultCommand;

public interface RegisterTournamentMatchResultUseCase {

  void handle(RegisterTournamentMatchResultCommand command);

}
