package com.villo.truco.domain.model.match.valueobjects.exceptions;

import com.villo.truco.domain.shared.DomainException;

public class InvalidIdException extends DomainException {

    public InvalidIdException(final String id) {

        super("Invalid id: " + id);
    }

}
