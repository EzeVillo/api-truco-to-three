package com.villo.truco.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LeagueTimeoutPropertiesTest {

  @Test
  void hasMutableTimeoutValues() {

    final var properties = new LeagueTimeoutProperties();
    properties.setIdleTimeoutSeconds(33);
    properties.setTimeoutCheckIntervalMs(44L);

    assertThat(properties.getIdleTimeoutSeconds()).isEqualTo(33);
    assertThat(properties.getTimeoutCheckIntervalMs()).isEqualTo(44L);
  }

}
