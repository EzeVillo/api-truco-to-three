package com.villo.truco.infrastructure.http.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado de disponibilidad del proceso backend para el front (wake/cold-start)")
public record WakeResponse(
    @Schema(description = "'ready' cuando el proceso acepta requests", example = "ready") String status) {

  public static final String READY = "ready";

  public static WakeResponse ready() {

    return new WakeResponse(READY);
  }

}
