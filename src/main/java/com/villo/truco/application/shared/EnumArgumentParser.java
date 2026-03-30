package com.villo.truco.application.shared;

import com.villo.truco.application.exceptions.InvalidEnumValueException;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public final class EnumArgumentParser {

  private EnumArgumentParser() {

  }

  public static <E extends Enum<E>> E parse(final Class<E> enumClass, final String field,
      final String value) {

    Objects.requireNonNull(enumClass, "Enum type is required");
    Objects.requireNonNull(field, "Field is required");
    Objects.requireNonNull(value, "Value is required");

    try {
      return Enum.valueOf(enumClass, value);
    } catch (final IllegalArgumentException ex) {
      throw new InvalidEnumValueException(field, value, allowedValues(enumClass));
    }
  }

  private static <E extends Enum<E>> String allowedValues(final Class<E> enumClass) {

    return Arrays.stream(enumClass.getEnumConstants()).map(Enum::name)
        .collect(Collectors.joining(", "));
  }

}
