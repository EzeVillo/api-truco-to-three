package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.CreateCupCommand;
import com.villo.truco.application.dto.CreateCupDTO;

public interface CreateCupUseCase extends UseCase<CreateCupCommand, CreateCupDTO> {

}
