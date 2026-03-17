package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.StartTournamentCommand;

public interface StartTournamentUseCase {

  void handle(StartTournamentCommand command);

}
