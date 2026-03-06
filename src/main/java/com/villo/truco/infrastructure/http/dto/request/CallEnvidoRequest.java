package com.villo.truco.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Solicitud para cantar envido")
public record CallEnvidoRequest(
    @Schema(description = "Tipo de canto envido", example = "ENVIDO") String call) {

  public CallEnvidoRequest {

    Objects.requireNonNull(call, "Call is required");
  }

}
