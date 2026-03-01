package com.villo.truco.domain.model.tournament.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class WinnerNotInFixtureException extends DomainException {

    public WinnerNotInFixtureException() {

        super("Winner does not belong to fixture players");
    }

}
