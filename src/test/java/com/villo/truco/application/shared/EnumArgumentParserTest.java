package com.villo.truco.application.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.application.exceptions.InvalidEnumValueException;
import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EnumArgumentParser")
class EnumArgumentParserTest {

  @Test
  @DisplayName("parsea correctamente un valor de enum valido")
  void parsesValidEnumValue() {

    final var parsed = EnumArgumentParser.parse(EnvidoCall.class, "call", "ENVIDO");

    assertThat(parsed).isEqualTo(EnvidoCall.ENVIDO);
  }

  @Test
  @DisplayName("lanza InvalidEnumValueException con detalle cuando el valor es invalido")
  void throwsCustomExceptionForInvalidValue() {

    assertThatThrownBy(
        () -> EnumArgumentParser.parse(EnvidoCall.class, "call", "INVALIDO")).isInstanceOf(
        InvalidEnumValueException.class).hasMessage(
        "Invalid value 'INVALIDO' for field 'call'. Allowed values: ENVIDO, REAL_ENVIDO, FALTA_ENVIDO");
  }

}
