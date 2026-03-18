package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.CreateLeagueCommand;
import com.villo.truco.application.dto.CreateLeagueDTO;

public interface CreateLeagueUseCase extends UseCase<CreateLeagueCommand, CreateLeagueDTO> {

}
