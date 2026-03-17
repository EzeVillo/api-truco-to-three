package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.JoinTournamentCommand;
import com.villo.truco.application.dto.JoinTournamentDTO;

public interface JoinTournamentUseCase {

  JoinTournamentDTO handle(JoinTournamentCommand command);

}
