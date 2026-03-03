package com.villo.truco.application.commands;

import java.util.Objects;

public record ExchangeSessionGrantCommand(String sessionGrant) {

  public ExchangeSessionGrantCommand {

    Objects.requireNonNull(sessionGrant, "SessionGrant is required");
  }

}
