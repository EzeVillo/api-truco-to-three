package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.ChatMessageDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Mensaje del chat")
public record ChatMessageResponse(
    @Schema(description = "ID del mensaje", example = "e3b0c442-98fc-1c14-b39f-f1b2d593a7c1") String messageId,
    @Schema(description = "Nombre visible del remitente", example = "juancho") String sender,
    @Schema(description = "Contenido del mensaje", example = "¡Truco!") String content,
    @Schema(description = "Timestamp del envío en milisegundos epoch", example = "1774886400000") long sentAt) {

  public static ChatMessageResponse from(final ChatMessageDTO dto) {

    return new ChatMessageResponse(dto.messageId(), dto.sender(), dto.content(), dto.sentAt());
  }

}
