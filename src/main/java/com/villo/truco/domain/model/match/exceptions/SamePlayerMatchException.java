package com.villo.truco.domain.model.match.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class SamePlayerMatchException extends DomainException {

    public SamePlayerMatchException() {

        super("A match cannot be created with the same player twice");
    }

}
