package com.villo.truco.infrastructure.http.dto.request;

import java.util.Objects;

public record RespondTrucoRequest(String response) {

  public RespondTrucoRequest {

    Objects.requireNonNull(response);
  }

}
