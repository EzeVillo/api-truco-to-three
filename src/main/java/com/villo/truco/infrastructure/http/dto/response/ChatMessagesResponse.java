package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.ChatMessagesDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Estado del chat con mensajes")
public record ChatMessagesResponse(
    @Schema(description = "ID del chat", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479") String chatId,
    @Schema(description = "Tipo de recurso padre", example = "MATCH") String parentType,
    @Schema(description = "ID del recurso padre", example = "a1b2c3d4-5678-9abc-def0-1234567890ab") String parentId,
    @Schema(description = "Mensajes del chat (máximo 50)") List<ChatMessageResponse> messages) {

  public static ChatMessagesResponse from(final ChatMessagesDTO dto) {

    return new ChatMessagesResponse(dto.chatId(), dto.parentType(), dto.parentId(),
        dto.messages().stream().map(ChatMessageResponse::from).toList());
  }

}
