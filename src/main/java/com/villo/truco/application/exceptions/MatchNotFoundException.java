package com.villo.truco.application.exceptions;

import com.villo.truco.domain.model.match.valueobjects.MatchId;

public final class MatchNotFoundException extends ApplicationException {

    public MatchNotFoundException(final MatchId matchId) {

        super(ApplicationStatus.NOT_FOUND, "Match not found: " + matchId);
    }

}
