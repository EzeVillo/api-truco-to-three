package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.SpectateMatchCommand;
import com.villo.truco.application.dto.SpectatorMatchStateDTO;

public interface SpectateMatchUseCase extends
    UseCase<SpectateMatchCommand, SpectatorMatchStateDTO> {

}
