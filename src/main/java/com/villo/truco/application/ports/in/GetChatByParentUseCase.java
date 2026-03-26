package com.villo.truco.application.ports.in;

import com.villo.truco.application.dto.ChatMessagesDTO;
import com.villo.truco.application.queries.GetChatByParentQuery;

public interface GetChatByParentUseCase extends UseCase<GetChatByParentQuery, ChatMessagesDTO> {

}
