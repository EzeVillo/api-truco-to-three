package com.villo.truco.application.exceptions;

import com.villo.truco.domain.shared.valueobjects.JoinCode;
import com.villo.truco.domain.shared.valueobjects.MatchId;

public final class MatchNotFoundException extends ApplicationException {

  public MatchNotFoundException(final MatchId matchId) {

    super(ApplicationStatus.NOT_FOUND, "Match not found: " + matchId);
  }

  public MatchNotFoundException(final JoinCode joinCode) {

    super(ApplicationStatus.NOT_FOUND, "Match not found for join code: " + joinCode.value());
  }

}
