package com.villo.truco.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MatchTimeoutPropertiesTest {

  @Test
  void hasMutableTimeoutValues() {

    final var properties = new MatchTimeoutProperties();
    properties.setIdleTimeoutSeconds(55);
    properties.setTimeoutCheckIntervalMs(66L);

    assertThat(properties.getIdleTimeoutSeconds()).isEqualTo(55);
    assertThat(properties.getTimeoutCheckIntervalMs()).isEqualTo(66L);
  }

}
