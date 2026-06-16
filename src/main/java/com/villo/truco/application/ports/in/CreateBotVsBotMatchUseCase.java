package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.CreateBotVsBotMatchCommand;
import com.villo.truco.application.dto.CreateBotVsBotMatchDTO;

public interface CreateBotVsBotMatchUseCase
    extends UseCase<CreateBotVsBotMatchCommand, CreateBotVsBotMatchDTO> {

}
