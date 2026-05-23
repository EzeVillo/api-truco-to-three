package com.villo.truco.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CupTimeoutPropertiesTest {

  @Test
  void hasMutableTimeoutValues() {

    final var properties = new CupTimeoutProperties();
    properties.setIdleTimeoutSeconds(11);

    assertThat(properties.getIdleTimeoutSeconds()).isEqualTo(11);
  }

}
