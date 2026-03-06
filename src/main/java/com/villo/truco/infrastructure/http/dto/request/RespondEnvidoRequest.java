package com.villo.truco.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Solicitud para responder un canto de envido")
public record RespondEnvidoRequest(
    @Schema(description = "Respuesta al envido", example = "QUIERO") String response) {

  public RespondEnvidoRequest {

    Objects.requireNonNull(response, "Response is required");
  }

}
