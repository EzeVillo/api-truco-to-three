package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.ExchangeSessionGrantCommand;
import com.villo.truco.application.dto.SessionTokenDTO;

public interface ExchangeSessionGrantUseCase {

  SessionTokenDTO handle(ExchangeSessionGrantCommand command);

}
