package com.villo.truco.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitud para enviar un mensaje en el chat")
public record SendMessageRequest(
    @NotBlank @Size(max = 500) @Schema(description = "Contenido del mensaje", example = "Buena mano!") String content) {

}
