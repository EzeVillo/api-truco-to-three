package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.SendMessageToParentCommand;
import com.villo.truco.application.dto.SendMessageResultDTO;

public interface SendMessageToParentUseCase extends
    UseCase<SendMessageToParentCommand, SendMessageResultDTO> {

}
