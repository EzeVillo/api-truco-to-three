package com.villo.truco.domain.model.match.exceptions;

import com.villo.truco.domain.model.match.valueobjects.RoundStatus;
import com.villo.truco.domain.shared.DomainException;
import java.util.List;

public final class InvalidRoundStateException extends DomainException {

    public InvalidRoundStateException(final RoundStatus current, final RoundStatus expected) {

        super("Invalid round state. Current: " + current + ", expected: " + expected);
    }

    public InvalidRoundStateException(final RoundStatus current, final List<RoundStatus> expected) {

        super("Invalid round state. Current: " + current + ", expected one of: " + expected);
    }

}
