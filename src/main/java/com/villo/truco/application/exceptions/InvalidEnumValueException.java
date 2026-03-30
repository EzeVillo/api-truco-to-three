package com.villo.truco.application.exceptions;

public final class InvalidEnumValueException extends ApplicationException {

  public InvalidEnumValueException(final String field, final String value,
      final String allowedValues) {

    super(ApplicationStatus.BAD_REQUEST,
        "Invalid value '" + value + "' for field '" + field + "'. Allowed values: "
            + allowedValues);
  }

}
