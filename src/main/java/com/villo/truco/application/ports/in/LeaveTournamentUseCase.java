package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.LeaveTournamentCommand;

public interface LeaveTournamentUseCase {

  void handle(LeaveTournamentCommand command);

}
