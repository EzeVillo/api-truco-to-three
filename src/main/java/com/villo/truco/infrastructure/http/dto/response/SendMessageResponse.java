package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.SendMessageResultDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta al enviar mensaje")
public record SendMessageResponse(
    @Schema(description = "ID del chat", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479") String chatId,
    @Schema(description = "Estado de envio actualizado para el remitente") ChatSendStateResponse sendState) {

  public static SendMessageResponse from(final SendMessageResultDTO dto) {

    return new SendMessageResponse(dto.chatId(), ChatSendStateResponse.from(dto.sendState()));
  }

}
