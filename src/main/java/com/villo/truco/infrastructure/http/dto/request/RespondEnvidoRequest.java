package com.villo.truco.infrastructure.http.dto.request;

import java.util.Objects;

public record RespondEnvidoRequest(String response) {

  public RespondEnvidoRequest {

    Objects.requireNonNull(response, "Response is required");
  }

}
