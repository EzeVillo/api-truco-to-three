package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.LoginCommand;
import com.villo.truco.application.dto.LoginDTO;

public interface LoginUseCase {

  LoginDTO handle(LoginCommand command);

}
