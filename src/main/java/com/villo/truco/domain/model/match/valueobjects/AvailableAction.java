package com.villo.truco.domain.model.match.valueobjects;

import java.util.Optional;

public record AvailableAction(ActionType type, String parameter) {

  public static AvailableAction of(final ActionType type, final String parameter) {

    return new AvailableAction(type, parameter);
  }

  public static AvailableAction of(final ActionType type) {

    return new AvailableAction(type, null);
  }

  public Optional<String> getParameter() {

    return Optional.ofNullable(this.parameter);
  }

}
