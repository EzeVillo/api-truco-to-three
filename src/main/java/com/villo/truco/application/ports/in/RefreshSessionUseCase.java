package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.RefreshSessionCommand;
import com.villo.truco.application.dto.SessionTokenDTO;

public interface RefreshSessionUseCase {

  SessionTokenDTO handle(RefreshSessionCommand command);

}
