package com.villo.truco.application.exceptions;

import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.InviteCode;

public final class MatchNotFoundException extends ApplicationException {

  public MatchNotFoundException(final MatchId matchId) {

    super(ApplicationStatus.NOT_FOUND, "Match not found: " + matchId);
  }

  public MatchNotFoundException(final InviteCode inviteCode) {

    super(ApplicationStatus.NOT_FOUND, "Match not found for invite code: " + inviteCode.value());
  }

}
