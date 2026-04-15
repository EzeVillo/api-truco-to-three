package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.SendMessageToParentCommand;
import com.villo.truco.domain.model.chat.valueobjects.ChatId;

public interface SendMessageToParentUseCase extends UseCase<SendMessageToParentCommand, ChatId> {

}
