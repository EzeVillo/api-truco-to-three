package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.JoinMatchCommand;

public interface JoinMatchUseCase {

    void handle(JoinMatchCommand command);

}
