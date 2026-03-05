package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.CreateMatchCommand;
import com.villo.truco.application.dto.CreateMatchDTO;

public interface CreateMatchUseCase {

  CreateMatchDTO handle(CreateMatchCommand command);

}
