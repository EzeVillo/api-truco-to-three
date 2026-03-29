package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.CreateBotMatchCommand;
import com.villo.truco.application.dto.CreateBotMatchDTO;

public interface CreateBotMatchUseCase extends UseCase<CreateBotMatchCommand, CreateBotMatchDTO> {

}
