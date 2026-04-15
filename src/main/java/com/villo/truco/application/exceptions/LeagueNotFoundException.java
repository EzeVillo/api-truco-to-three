package com.villo.truco.application.exceptions;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.valueobjects.JoinCode;

public final class LeagueNotFoundException extends ApplicationException {

  public LeagueNotFoundException(final LeagueId leagueId) {

    super(ApplicationStatus.NOT_FOUND, "League not found: " + leagueId.value());
  }

  public LeagueNotFoundException(final JoinCode joinCode) {

    super(ApplicationStatus.NOT_FOUND, "League not found for join code: " + joinCode.value());
  }

}
