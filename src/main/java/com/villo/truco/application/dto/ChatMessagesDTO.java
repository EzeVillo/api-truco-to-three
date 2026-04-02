package com.villo.truco.application.dto;

import java.util.List;

public record ChatMessagesDTO(String chatId, String parentType, String parentId,
                              List<ChatMessageDTO> messages) {

}
