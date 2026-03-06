package com.villo.truco.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Solicitud para responder un canto de truco")
public record RespondTrucoRequest(
    @Schema(description = "Respuesta al truco", example = "QUIERO") String response) {

  public RespondTrucoRequest {

    Objects.requireNonNull(response);
  }

}
