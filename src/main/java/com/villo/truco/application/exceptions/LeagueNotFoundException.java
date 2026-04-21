package com.villo.truco.application.exceptions;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;

public final class LeagueNotFoundException extends ApplicationException {

  public LeagueNotFoundException(final LeagueId leagueId) {

    super(ApplicationStatus.NOT_FOUND, "League not found: " + leagueId.value());
  }

}
