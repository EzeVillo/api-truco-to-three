package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.CreateTournamentCommand;
import com.villo.truco.application.dto.CreateTournamentDTO;

public interface CreateTournamentUseCase extends
    UseCase<CreateTournamentCommand, CreateTournamentDTO> {

}
