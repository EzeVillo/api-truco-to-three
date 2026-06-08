package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.ChatSendStateDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado de envio del jugador autenticado en el chat")
public record ChatSendStateResponse(
    @Schema(description = "Indica si el jugador autenticado puede enviar ahora", example = "false") boolean canSendNow,
    @Schema(description = "Epoch millis del proximo envio permitido; null si puede enviar ahora", example = "1772768160123", nullable = true) Long nextMessageAllowedAt) {

  public static ChatSendStateResponse from(final ChatSendStateDTO dto) {

    return new ChatSendStateResponse(dto.canSendNow(), dto.nextMessageAllowedAt());
  }

}
