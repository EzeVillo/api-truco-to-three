package com.villo.truco.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LeagueTimeoutPropertiesTest {

  @Test
  void hasMutableTimeoutValues() {

    final var properties = new LeagueTimeoutProperties();
    properties.setLobbyTimeoutSeconds(33);

    assertThat(properties.getLobbyTimeoutSeconds()).isEqualTo(33);
  }

}
