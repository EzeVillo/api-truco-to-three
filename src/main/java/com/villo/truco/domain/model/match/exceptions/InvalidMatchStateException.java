package com.villo.truco.domain.model.match.exceptions;

import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import com.villo.truco.domain.shared.DomainException;
import java.util.Arrays;

public final class InvalidMatchStateException extends DomainException {

  public InvalidMatchStateException(final MatchStatus current, final MatchStatus expected) {

    super("Invalid match state. Current: " + current + ", expected: " + expected);
  }

  public InvalidMatchStateException(final MatchStatus current, final MatchStatus... expected) {

    super("Invalid match state. Current: " + current + ", expected one of: " + Arrays.toString(
        expected));
  }

}
