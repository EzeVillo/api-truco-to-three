package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.RespondEnvidoCommand;

public interface RespondEnvidoUseCase {

  void handle(RespondEnvidoCommand command);

}
