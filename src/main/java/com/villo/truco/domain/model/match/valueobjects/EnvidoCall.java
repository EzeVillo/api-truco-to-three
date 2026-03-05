package com.villo.truco.domain.model.match.valueobjects;

public enum EnvidoCall {
  ENVIDO, REAL_ENVIDO, FALTA_ENVIDO;

  public int points() {

    return switch (this) {
      case ENVIDO -> 2;
      case REAL_ENVIDO -> 3;
      case FALTA_ENVIDO -> -1;
    };
  }

  public boolean canBeRaisedWith(final EnvidoCall other) {

    return switch (this) {
      case ENVIDO -> true;
      case REAL_ENVIDO -> other == FALTA_ENVIDO;
      case FALTA_ENVIDO -> false;
    };
  }
}