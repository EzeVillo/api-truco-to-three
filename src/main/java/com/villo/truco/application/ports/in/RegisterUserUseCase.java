package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.RegisterUserCommand;
import com.villo.truco.application.dto.RegisterUserDTO;

public interface RegisterUserUseCase extends UseCase<RegisterUserCommand, RegisterUserDTO> {

}
