package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.JoinMatchCommand;
import com.villo.truco.application.dto.JoinMatchDTO;

public interface JoinMatchUseCase {

  JoinMatchDTO handle(JoinMatchCommand command);

}
