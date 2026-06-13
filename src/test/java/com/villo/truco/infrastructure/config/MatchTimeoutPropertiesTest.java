package com.villo.truco.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MatchTimeoutPropertiesTest {

  @Test
  void hasMutableTimeoutValues() {

    final var properties = new MatchTimeoutProperties();
    properties.setLobbyTimeoutSeconds(55);
    properties.setPlayTimeoutSeconds(22);

    assertThat(properties.getLobbyTimeoutSeconds()).isEqualTo(55);
    assertThat(properties.getPlayTimeoutSeconds()).isEqualTo(22);
  }

  @Test
  void hasSensibleDefaults() {

    final var properties = new MatchTimeoutProperties();

    assertThat(properties.getLobbyTimeoutSeconds()).isEqualTo(300);
    assertThat(properties.getPlayTimeoutSeconds()).isEqualTo(30);
  }

}
