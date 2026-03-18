package com.villo.truco.application.exceptions;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.valueobjects.InviteCode;

public final class LeagueNotFoundException extends ApplicationException {

  public LeagueNotFoundException(final LeagueId leagueId) {

    super(ApplicationStatus.NOT_FOUND, "League not found: " + leagueId.value());
  }

  public LeagueNotFoundException(final InviteCode inviteCode) {

    super(ApplicationStatus.NOT_FOUND, "League not found for invite code: " + inviteCode.value());
  }

}
