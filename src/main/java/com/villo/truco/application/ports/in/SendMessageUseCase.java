package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.SendMessageCommand;
import com.villo.truco.application.dto.SendMessageResultDTO;

public interface SendMessageUseCase extends UseCase<SendMessageCommand, SendMessageResultDTO> {

}
