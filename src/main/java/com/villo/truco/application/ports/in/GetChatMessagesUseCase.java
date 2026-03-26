package com.villo.truco.application.ports.in;

import com.villo.truco.application.dto.ChatMessagesDTO;
import com.villo.truco.application.queries.GetChatMessagesQuery;

public interface GetChatMessagesUseCase extends UseCase<GetChatMessagesQuery, ChatMessagesDTO> {

}
