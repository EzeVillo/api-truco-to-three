package com.villo.truco.infrastructure.http.dto.request;

import java.util.Objects;

public record CallEnvidoRequest(String call) {

  public CallEnvidoRequest {

    Objects.requireNonNull(call, "Call is required");
  }

}
