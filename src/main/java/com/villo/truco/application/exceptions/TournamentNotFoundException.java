package com.villo.truco.application.exceptions;

import com.villo.truco.domain.model.tournament.valueobjects.TournamentId;

public final class TournamentNotFoundException extends ApplicationException {

    public TournamentNotFoundException(final TournamentId tournamentId) {

        super(ApplicationStatus.NOT_FOUND, "Tournament not found: " + tournamentId.value());
    }

}
