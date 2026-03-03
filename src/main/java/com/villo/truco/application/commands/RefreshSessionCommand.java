package com.villo.truco.application.commands;

import java.util.Objects;

public record RefreshSessionCommand(String refreshToken) {

  public RefreshSessionCommand {

    Objects.requireNonNull(refreshToken, "RefreshToken is required");
  }

}
