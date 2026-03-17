package com.villo.truco.application.exceptions;

import com.villo.truco.domain.model.tournament.valueobjects.TournamentId;
import com.villo.truco.domain.shared.valueobjects.InviteCode;

public final class TournamentNotFoundException extends ApplicationException {

  public TournamentNotFoundException(final TournamentId tournamentId) {

    super(ApplicationStatus.NOT_FOUND, "Tournament not found: " + tournamentId.value());
  }

  public TournamentNotFoundException(final InviteCode inviteCode) {

    super(ApplicationStatus.NOT_FOUND,
        "Tournament not found for invite code: " + inviteCode.value());
  }

}
