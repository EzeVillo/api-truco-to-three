package com.villo.truco.application.dto;

import java.util.List;

public record ChatMessagesDTO(String chatId, String parentType, String parentId,
                              ChatSendStateDTO sendState, List<ChatMessageDTO> messages) {

}
