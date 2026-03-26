package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.SendMessageCommand;
import com.villo.truco.domain.model.chat.valueobjects.ChatId;

public interface SendMessageUseCase extends UseCase<SendMessageCommand, ChatId> {

}
