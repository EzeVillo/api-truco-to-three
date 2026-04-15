package com.villo.truco.infrastructure.http.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta al enviar mensaje por recurso padre")
public record SendMessageToParentResponse(
    @Schema(description = "ID del chat", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479") String chatId) {

}
